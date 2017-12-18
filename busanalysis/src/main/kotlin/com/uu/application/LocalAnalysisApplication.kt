package com.uu.application

import com.jfinal.plugin.redis.Redis
import com.uu.common.DecisionTreeClassifier
import com.uu.common.TracingPointAnalyzer
import com.uu.common.constant.Consts
import com.uu.common.kit.AMapKit
import com.uu.common.kit.CurveFitKit
import com.uu.common.kit.ToolKit
import com.uu.common.proxy.DbBase
import com.uu.common.proxy.DbSubject
import com.uu.hadoop.bean.CarPoint
import com.uu.hadoop.bean.LineDirection
import com.uu.hadoop.bean.Point
import com.uu.hadoop.bean.Station
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by song27 on 2017/11/22 0022.
 */
object LocalAnalysisApplication : DbSubject {
    /**
     * 本地运行版本
     */
    override fun onStart() {
        /**
         * ****************************************************************
         * ****************************************************************
         * 装载数据 start
         *
         */
        val cache = Redis.use(Consts.JEDIS_UUAPP)!!

        val keys = cache.keys("station:*")

        val list: List<Station> = cache.mget(*keys.toTypedArray()) as MutableList<Station>

        val carPoints: ArrayList<CarPoint> = ArrayList()

        val file = File(Consts.LOCAL_SOURCE_INPUT_PATH)

        var point: List<String>

        val format = SimpleDateFormat("YYYY-MM-DD HH:mm:ss")

        var prePoint: CarPoint? = null

        file.forEachLine {
            point = it.split(",")
            var lat = point[1].toDouble()
            var lon = point[2].toDouble()
            if (prePoint?.lat != lat || prePoint?.lon != lon){
                val carPoint = CarPoint()
                carPoint.carPlate = point[3]
                carPoint.lat = point[1].toDouble()
                carPoint.lon = point[2].toDouble()
                carPoint.time = format.parse(point[4]).time
                if (!carPoints.contains(carPoint))
                    carPoints.add(carPoint)
                prePoint = carPoint
            }
        }
        /**
         *
         * 装载数据 end
         * ****************************************************************
         * ****************************************************************
         */


        /**
         * ****************************************************************
         * ****************************************************************
         * 所属线路分析
         * 即：车辆A，输入车辆A走过的轨迹点经过了那些站点，分析出车辆A是20路
         * start
         *
         */
        var result = DecisionTreeClassifier.determineRoute(carPoints!!, list)
        /**
         *
         * 所属线路分析
         * end
         * ****************************************************************
         * ****************************************************************
         */


        /**
         * ****************************************************************
         * ****************************************************************
         * 轨迹点分析
         * 即：已知车辆A是20路，根据车辆A走过的定位数据分析出20路正向反向的路径轨迹
         * start
         *
         */
        val shuttleBusAndLineID = TreeMap<Int, Int>()
        val division = TracingPointAnalyzer.division(carPoints, result!!, shuttleBusAndLineID)
        var line = result.lines[LineDirection.POSITIVE]
        var shuttleBuses = shuttleBusAndLineID.filter { it.value == line?.id }

        val pointArrays = ArrayList<ArrayList<CarPoint>>()

        division.forEach { shuttleBus, arrayList ->
            if (shuttleBuses.containsKey(shuttleBus)) {
                pointArrays.add(arrayList)
            }
        }
        var locateDataSet = TracingPointAnalyzer.getTracingPointsByLocateDataSet(pointArrays, line!!)

        var lats = DoubleArray(locateDataSet.size)
        var lons = DoubleArray(locateDataSet.size)
        locateDataSet.forEachIndexed { index, carPoint ->
            lats[index] = carPoint.lat
            lons[index] = carPoint.lon
        }
        var curvePoints = CurveFitKit.getBSplineCurvePoints(lats, lons, lats.size - 1, 5)


        var firstPoint: Point? = null
        var lastPoint: Point? = null
        line.stations.forEachIndexed { index, station ->
            var point = Point(station.lat, station.lon)
            if (index == 0) firstPoint = point
            if (index == line!!.stations.size - 1) lastPoint = point
            TracingPointAnalyzer.insertStation(curvePoints, point)
        }

        println("***************************************")
        println("line.id:${result!!.lines[LineDirection.POSITIVE]?.id}")
        println("[")
        for (curvePoint in curvePoints.subList(curvePoints.indexOf(firstPoint), curvePoints.indexOf(lastPoint))) {
            val wgs84ToGcj02 = AMapKit.wgs84ToGcj02(curvePoint.lat, curvePoint.lon)
            var lat = wgs84ToGcj02["lat"]!!
            var lon = wgs84ToGcj02["lon"]!!
            print("{\"lon\":$lon,\"lat\":$lat},")
        }
        println("]")
        println("***************************************")


        line = result.lines[LineDirection.REVERSE]
        shuttleBuses = shuttleBusAndLineID.filter { it.value == line?.id }
        division.forEach { shuttleBus, arrayList ->
            if (shuttleBuses.containsKey(shuttleBus)) {
                pointArrays.add(arrayList)
            }
        }
        locateDataSet = TracingPointAnalyzer.getTracingPointsByLocateDataSet(pointArrays, line!!)

        lats = DoubleArray(locateDataSet.size)
        lons = DoubleArray(locateDataSet.size)
        locateDataSet.forEachIndexed { index, carPoint ->
            lats[index] = carPoint.lat
            lons[index] = carPoint.lon
        }
        curvePoints = CurveFitKit.getBSplineCurvePoints(lats, lons, lats.size - 1, 5)


        line.stations.forEachIndexed { index, station ->
            var point = Point(station.lat, station.lon)
            if (index == 0) firstPoint = point
            if (index == line!!.stations.size - 1) lastPoint = point
            TracingPointAnalyzer.insertStation(curvePoints, point)
        }

        println("***************************************")
        println("line.id:${result!!.lines[LineDirection.REVERSE]?.id}")
        println("[")
        for (curvePoint in curvePoints.subList(curvePoints.indexOf(firstPoint), curvePoints.indexOf(lastPoint))) {
            val wgs84ToGcj02 = AMapKit.wgs84ToGcj02(curvePoint.lat, curvePoint.lon)
            var lat = wgs84ToGcj02["lat"]!!
            var lon = wgs84ToGcj02["lon"]!!
            print("[$lon,$lat],")
        }
        println("]")
        println("***************************************")
        /**
         *
         * 轨迹点分析
         * end
         * ****************************************************************
         * ****************************************************************
         */
    }
}

fun main(args: Array<String>) {
    val subject = DbBase(LocalAnalysisApplication).proxy as DbSubject
    subject.onStart()
}