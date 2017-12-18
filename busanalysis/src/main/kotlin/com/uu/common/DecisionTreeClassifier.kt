package com.uu.common

import com.jfinal.plugin.redis.Redis
import com.uu.common.constant.Consts
import com.uu.common.exception.LineException
import com.uu.common.kit.ToolKit
import com.uu.hadoop.bean.*

/**
 * Created by song27 on 2017/8/14 0014.
 *
 * 公交车辆所属线路判定分类器
 */
object DecisionTreeClassifier : Classifier {
    private val cache = Redis.use(Consts.JEDIS_UUAPP)!!
    private val lines = HashSet<Line>()
    private val throughStations = ArrayList<Station>()
    private val tempScreenOutStations = HashSet<Station>()
    private val tempStation = ArrayList<Station>()

    /**
     * 公交车辆分类器（区分线路）
     *
     * @param carPoints         :公交定位点
     * @param screenOutStations :选取的站点
     * @return Result是分析结果，里面包含了所分析出的线路，靠近公交站点的定位点
     */
    override fun determineRoute(carPoints: List<CarPoint>, screenOutStations: List<Station>): Result? {

        /**
         * ****************************************************************
         * ****************************************************************
         * 初步线路站点分析 start
         * 分析出可能经过的站点与可能有关的线路
         *
         */
        tempScreenOutStations.addAll(screenOutStations)
        val result = Result()
        var distance: Long = 0

        carPoints.forEach({ carPoint ->
            //计算车辆经过的定位形成的路线经过了那些站点
            tempScreenOutStations.forEach { station ->
                distance = Math.round(ToolKit.getDistance(station.lat, station.lon, carPoint.lat, carPoint.lon))

                if (distance <= 150) {
                    if (!throughStations.contains(station)) {
                        throughStations.add(station)
                    }
                    return@forEach
                }
            }
        })

        throughStations.forEach {
            it.lines.forEach { busID ->
                lines.add(cache["line:$busID"])
            }
        }
        /**
         *
         * 初步线路站点分析 end
         * ****************************************************************
         * ****************************************************************
         */


        /**
         * ****************************************************************
         * ****************************************************************
         * 权重分析 start
         * 分析出可能经过的站点与可能有关的线路
         *
         */

        //加权轮询，计算最大权重
        var maxWeight = 0.0
        var tempWeight: Double
        lines.forEach {
            tempStation.addAll(throughStations)
            tempStation.retainAll(it.stations)
            tempWeight = tempStation.size.toDouble() * (1.toDouble() / (Math.abs(it.stations.size - tempStation.size) + 1).toDouble())
            if (tempWeight >= maxWeight) {
                maxWeight = tempWeight
            }
            tempStation.clear()
        }

        //小于最大权重的剔除，也就是选择经过的站点符合度最高的线路
        val iterator = lines.iterator()
        iterator.forEach {
            tempStation.addAll(throughStations)
            tempStation.retainAll(it.stations)
            tempWeight = tempStation.size.toDouble() * (1.toDouble() / (Math.abs(it.stations.size - tempStation.size) + 1).toDouble())
            if (tempWeight < maxWeight) {
                iterator.remove()
            }
            tempStation.clear()
        }

        /**
         * 判断是否已经抉择出唯一一条线路
         */
        when {
            lines.size == 2 -> {
                var isSame = false
                var pre: Line? = null
                lines.forEach {
                    if (pre == null) {
                        pre = it
                    } else if (pre?.name.equals(it.name)) {
                        result.lines.put(LineDirection.POSITIVE, lines.first())
                        result.lines.put(LineDirection.REVERSE, lines.last())
                        isSame = true
                    }
                }
                if (!isSame) {
                    throw LineException("This car ${carPoints[0].carPlate} is more than one bus line.")
                }
            }
            lines.size > 2 -> {
                throw LineException("This car ${carPoints[0].carPlate} is more than two bus line.")
            }
            lines.size <= 0 -> {
                throw LineException("This car ${carPoints[0].carPlate} is not sure which line belongs to")
            }
            lines.size == 1 -> {
                result.lines.put(LineDirection.POSITIVE, lines.first())
                var reverse: Line? = cache["line:${lines.first().id + 1}"]
                if (reverse?.name != lines.first().name) {
                    reverse = cache["line:${lines.first().id - 1}"]
                }
                result.lines.put(LineDirection.REVERSE, reverse!!)
            }
        }
        /**
         *
         * 权重分析 end
         * ****************************************************************
         * ****************************************************************
         */


        /**
         * ****************************************************************
         * ****************************************************************
         * 提取靠近站点的定位数据 start
         * 把与站点距离大于100米的定位数据剔除，不纳入靠近站点的定位数据列表
         *
         */
        //获取经过某些站点的定位点
        tempScreenOutStations.clear()
        tempScreenOutStations.addAll(result.lines[LineDirection.POSITIVE]?.stations!!)
        tempScreenOutStations.addAll(result.lines[LineDirection.REVERSE]?.stations!!)

        carPoints.forEachIndexed { index, carPoint ->
            tempScreenOutStations.forEach { station ->
                val stationNearCarPoint = StationNearCarPoint()
                distance = Math.round(ToolKit.getDistance(station.lat, station.lon, carPoint.lat, carPoint.lon))
                stationNearCarPoint.distance = distance
                stationNearCarPoint.station = station
                if (distance <= 100) {
                    if ((!result.stationNearCarPoints.containsKey(index) && !result.stationNearCarPoints.containsValue(stationNearCarPoint)) ||
                            (result.stationNearCarPoints.containsKey(index) && distance < result.stationNearCarPoints[index]?.distance!!) ||
                            !result.stationNearCarPoints.containsKey(index)) {
                        result.stationNearCarPoints.put(index, stationNearCarPoint)
                    }
                }
            }
        }
        /**
         *
         * 提取靠近站点的定位数据 end
         * ****************************************************************
         * ****************************************************************
         */

        lines.clear()
        throughStations.clear()
        tempScreenOutStations.clear()
        tempStation.clear()
        return result
    }
}