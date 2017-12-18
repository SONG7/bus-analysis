package com.uu.common

import com.uu.common.constant.Consts
import com.uu.common.kit.*
import com.uu.hadoop.bean.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * Created by song27 on 2017/8/23 0023.
 *
 * 轨迹点分析器
 */
object TracingPointAnalyzer {
    /**
     * 用于分析的参考数据
     */
    data class Reference(val referencePoints: ArrayList<CarPoint> = ArrayList(1000),
                         val refAngles: HashMap<Int, Double> = HashMap(),
                         val turningPoints: ArrayList<CarPoint> = ArrayList(1000))

    private val linearResults = HashMap<Int, LinearResult>()
    private val firstStation = CarPoint()
    private val lastStation = CarPoint()

    /**
     * 定位数据预处理——分割定位点集合并按班次分组
     *
     * @param carPoints             未分组处理的定位点集合
     * @param result                已分析出线路，站点的结果数据
     * @param shuttleBusAndLineID   班次对应的路线ID的映射(用于装载数据，new一个就行)
     */
    fun division(carPoints: ArrayList<CarPoint>, result: Result, shuttleBusAndLineID: TreeMap<Int, Int>): TreeMap<Int, ArrayList<CarPoint>> {
        var currentShuttleBus = 0
        var positiveLine = result.lines[LineDirection.POSITIVE]
        var reverseLine = result.lines[LineDirection.REVERSE]
        val turningPoints = ArrayList<CarPoint>()
        //分组
        var carPointGroups = TreeMap<Int, ArrayList<CarPoint>>()

        with(result) {
            /**
             * **************************************************************************
             * **************************************************************************
             * <title>插入首末站</title>
             * 向定位点集合插入首末站点，用来按照班次分组定位点，从出发站到目的站为一班次
             * start
             *
             */
            firstStation.lat = positiveLine?.stations?.first()?.lat!!
            firstStation.lon = positiveLine.stations.first().lon
            firstStation.nextStation = positiveLine.stations.first()
            firstStation.isStation = true

            lastStation.lat = positiveLine.stations.last().lat
            lastStation.lon = positiveLine.stations.last().lon
            lastStation.nextStation = positiveLine.stations.last()
            lastStation.isStation = true

            var referenceAngle = setReferenceCategory(carPoints!!, 0.6)

            carPoints.forEachIndexed { index, carPoint ->
                if (referenceAngle!!.containsKey(index) && referenceAngle!![index]!! < 0.75) {
                    turningPoints.add(carPoint)
                }
            }

            val firstMinDistanceMap = TreeMap<Int, CarPoint>()
            val lastMinDistanceMap = TreeMap<Int, CarPoint>()
            findMinDistancePoint(carPoints, lastMinDistanceMap, lastStation)
            findMinDistancePoint(carPoints, firstMinDistanceMap, firstStation)


            var tempIndex = ArrayList<Int>()
            var deviation = 0.0
            var firstAndEndPoint = ArrayList<Int>()
            var currentMinPoint = 0
            var currentMinDistance = 0.0
            var currentDistance: Double

            //执行填充首末站的函数对象
            val fillStartAndEndStation: (Int, CarPoint, Station) -> Unit = { t, u, station ->
                tempIndex.add(t)
                tempIndex.forEach {
                    deviation += Math.abs(it - tempIndex.average())
                }

                if (deviation / tempIndex.size > 10) {
                    firstAndEndPoint.add(currentMinPoint)
                    tempIndex.clear()
                    tempIndex.add(t)
                    carPoints[currentMinPoint].nextStation = station
                    stationNearCarPoints[currentMinPoint] = StationNearCarPoint(station = station)
                    currentMinDistance = 0.0
                    currentDistance = 0.0
                    currentMinPoint = 0
                }

                currentDistance = ToolKit.getDistance(station.lat, station.lon, u.lat, u.lon)
                if (currentMinPoint == 0) {
                    currentMinPoint = t
                    currentMinDistance = currentDistance
                } else if (currentDistance < currentMinDistance) {
                    currentMinPoint = t
                    currentMinDistance = currentDistance
                }
                deviation = 0.0
            }

            firstMinDistanceMap.forEach { t, u ->
                fillStartAndEndStation(t, u, positiveLine.stations.first())
            }

            tempIndex.clear()
            deviation = 0.0
            currentMinDistance = 0.0
            currentDistance = 0.0
            currentMinPoint = 0
            lastMinDistanceMap.forEach { t, u ->
                fillStartAndEndStation(t, u, positiveLine.stations.last())
            }
            /**
             * <title>插入首末站</title>
             * end
             * **************************************************************************
             * **************************************************************************
             */


            /**
             * **************************************************************************
             * **************************************************************************
             * <title>轨迹点班次赋值</title>
             * 为每一个轨迹点班次属性赋值
             * start
             *
             */
            //保存当前班次走过的站点，按行走方向排序
            var stationsOfOneShuttleBus = ArrayList<Station>()
            var stationsOfAllShuttleBus = ArrayList<Station>()

            var preStationIndex: Int = -1
            var currentStation: Station? = null
            var preStation: Station? = null
            var indexes = HashSet(stationNearCarPoints.keys)

            //划分班次，并为每个班次进行线路方向分类
            carPoints.forEachIndexed { index, carPoint ->
                carPoint.shuttleBus = currentShuttleBus

                //如果该公交定位点接近公交站点，则执行代码块中的代码
                if (indexes.contains(index)) {
                    currentStation = carPoint.nextStation
                    //判断班次所属线路的行走方向
                    if (currentStation?.name != stationNearCarPoints[index]?.station?.name) {
                        currentStation = stationNearCarPoints[index]?.station
                    }

                    //如果一个班次已经确定了是什么线路方向的，就不需要继续分析
                    if (!shuttleBusAndLineID.keys.contains(currentShuttleBus)) {
                        //不包含该站点才允许添加该站点
                        if (!stationsOfOneShuttleBus.contains(currentStation)) {
                            stationsOfOneShuttleBus.add(currentStation!!)
                        }

                        stationsOfOneShuttleBus.forEach {
                            val indexOf = positiveLine?.stations?.indexByNameOf(it)
                            if (preStationIndex == -1) {
                                preStationIndex = indexOf!!
                            }

                            if (indexOf!! < 0 || (preStationIndex != -1 && preStationIndex > indexOf!!)) {
                                shuttleBusAndLineID.put(currentShuttleBus, reverseLine?.id!!)
                                preStationIndex = -1
                            } else if (preStationIndex != -1 && preStationIndex < indexOf!!) {
                                shuttleBusAndLineID.put(currentShuttleBus, positiveLine?.id!!)
                                preStationIndex = -1
                            }
                        }
                    }

                    if (!stationsOfAllShuttleBus.contains(currentStation)) {
                        try {
                            stationsOfAllShuttleBus.add(currentStation!!)
                        } catch (e: Exception) {
                            println(currentStation)
                        }
                    }

                    val stationName = currentStation?.name
                    if (stationsOfAllShuttleBus.size > 1 && (stationName == positiveLine?.stations?.last()?.name
                            || stationName == reverseLine?.stations?.last()?.name)) {
                        if (preStation?.name != currentStation?.name) {
                            currentShuttleBus++
                            stationsOfOneShuttleBus.clear()
                        }
                    }
                    preStation = currentStation
                }
            }
            /**
             * <title>轨迹点班次赋值</title>
             * end
             * **************************************************************************
             * **************************************************************************
             */


            /**
             * **************************************************************************
             * **************************************************************************
             * <title>各班次轨迹点提取</title>
             * 向定位点集合插入首末站点，用来按照班次分组定位点，从出发站到目的站为一班次
             * start
             *
             */
            var preEntry: Map.Entry<Int, Int>? = null
            val shuttleBusMergeMap = HashMap<Int, ArrayList<Int>>()

            shuttleBusAndLineID.forEach {
                if (it.value == preEntry?.value) {
                    if (!shuttleBusMergeMap.containsKey(preEntry?.key)) {
                        shuttleBusMergeMap.put(preEntry?.key!!, ArrayList())
                    }
                    shuttleBusMergeMap[preEntry?.key]?.add(it.key)
                } else {
                    preEntry = it
                }
            }

            carPoints.filter { it.shuttleBus > 0 }.forEach {
                //班次误判合并
                shuttleBusMergeMap.forEach { k, v ->
                    if (v.contains(it.shuttleBus)) {
                        it.shuttleBus = k
                    }
                }
                if (!carPointGroups.keys.contains(it.shuttleBus)) {
                    carPointGroups[it.shuttleBus] = ArrayList()
                }
                carPointGroups[it.shuttleBus]?.add(it)
            }
        }
        /**
         *
         * <title>各班次轨迹点提取</title>
         * end
         * **************************************************************************
         * **************************************************************************
         */
        return carPointGroups
    }

    /**
     * 根据各组班次定位数据集，获得聚类成的一组代表性的定位数据集，也就是最符合道路中心线的定位数据
     * 尽量积攒较多的shuttleList数据集数据，数据越多，获得聚类的道路中心线越准确
     *
     * @param shuttleList   多个班次（同方向，即同一线路ID）定位数据集合的数据集合
     * @param line          由knn分类算法进行车辆所属线路分类之后返回的所属线路结果
     * @return              一组未拟合，由定位数据进行滚筒式聚类后的道路中心线数据集合
     */
    fun getTracingPointsByLocateDataSet(shuttleList: Collection<ArrayList<CarPoint>>, line: Line): ArrayList<CarPoint> {
        var tracingPoints = ArrayList<CarPoint>(10000)
        val firstStation = line.stations.first()
        var subPointSet = ArrayList<CarPoint>(1000)

        var firstPoint = CarPoint()
        firstPoint.lat = firstStation.lat
        firstPoint.lon = firstStation.lon
        firstPoint.isStation = true
        tracingPoints.add(firstPoint)

        var currentLastPoint: CarPoint
        var radius = 20.0
        var last: Int
        (0..30000).forEach {
            try {
                currentLastPoint = tracingPoints.last()
                //TODO:此处可以使用多线程进行优化速度
                getCluster(shuttleList, currentLastPoint, radius, subPointSet)

                if (subPointSet.size > 0) {
                    tracingPoints.addAll(WekaMLKit.tracingPointClustering(subPointSet))
                    subPointSet.clear()
                } else {
                    shuttleList.forEach {
                        if (it.any { carPoint -> !carPoint.read }) {
                            last = if (it.any { carPoint -> carPoint.read }) {
                                it.indexOf(it.last { carPoint -> carPoint.read })
                            } else 0
                            val filterPoints = it.filterIndexed { index, carPoint -> !carPoint.read && index > last }
                            if (filterPoints.isNotEmpty()) {
                                subPointSet.add(filterPoints.first())
                            }
                        }
                    }

                    if (subPointSet.size <= 0) {
                        return@forEach
                    }
                    currentLastPoint = subPointSet.minBy { carPoint -> ToolKit.getDistance(currentLastPoint.lat, currentLastPoint.lon, carPoint.lat, carPoint.lon) }!!
                    subPointSet.clear()
                    getCluster(shuttleList, currentLastPoint, radius, subPointSet)
                    tracingPoints.addAll(WekaMLKit.tracingPointClustering(subPointSet))
                    subPointSet.clear()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return tracingPoints
    }

    /**
     * 获得可聚类的集合
     */
    private fun getCluster(shuttleList: Collection<ArrayList<CarPoint>>, currentLastPoint: CarPoint, radius: Double, subPointSet: ArrayList<CarPoint>) {
        pointExtraction(shuttleList, { carPoint: CarPoint ->
            if (ToolKit.getDistance(currentLastPoint.lat, currentLastPoint.lon, carPoint.lat, carPoint.lon) <= radius) {
                carPoint.read = true
                subPointSet.add(carPoint)
            }
        })
    }

    /**
     * 班次定位点萃取
     */
    private fun pointExtraction(shuttleList: Collection<ArrayList<CarPoint>>, callBack: (carPoint: CarPoint) -> Unit) {
        shuttleList.forEach { shuttle ->
            shuttle.filter { carPoint -> !carPoint.read }.forEach { carPoint ->
                callBack(carPoint)
            }
        }
    }

    /**
     * 同线路同方向各班次合并,获得排序好的完整的一单方向线路的所有轨迹点
     * @param shuttleList   班次定位点列表
     * @param reference     分析参考对象
     */
    fun shuttleMerge(shuttleList: Collection<ArrayList<CarPoint>>, reference: Reference): ArrayList<CarPoint> {
        var points = ArrayList<CarPoint>(10000)
        shuttleList.forEach { shuttle ->
            if (points.size <= 0) {
                reference.referencePoints.addAll(shuttle)
                points.addAll(reference.referencePoints)

                reference.refAngles.putAll(setReferenceCategory(reference.referencePoints, 0.6))
            } else {
                insertSortOfCarPoint(reference.referencePoints, points, shuttle, reference.turningPoints, false)
            }
        }
        return points
    }

    /**
     * 获得分析出的轨迹点
     * @param points        一组同班次同方向定位点集合
     * @param reference     分析参考对象
     */
    fun getTracingPoints(points: ArrayList<CarPoint>, reference: Reference): ArrayList<CarPoint> {
        reference.referencePoints.forEachIndexed { index, carPoint ->
            if (reference.refAngles.containsKey(index) && reference.refAngles[index]!! < 0.75) {
                reference.turningPoints.add(carPoint)
            }
        }
        insert(reference.referencePoints, points, firstStation, -1, reference.turningPoints, true)
        insert(reference.referencePoints, points, lastStation, -1, reference.turningPoints, true)

        var tempPoints = ArrayList(points.subList(points.indexOf(lastStation), points.size))

        val pretreatmentPoints = PointPretreatmentKit.pretreatment(tempPoints)

        var lats = DoubleArray(pretreatmentPoints.size)
        var lons = DoubleArray(pretreatmentPoints.size)
        pretreatmentPoints.forEachIndexed { index, carPoint ->
            lats[index] = carPoint.lat
            lons[index] = carPoint.lon
        }
        println("[")
        for (curvePoint in CurveFitKit.getBSplineCurvePoints(lats, lons, lats.size - 1, 2)) {
            val wgs84ToGcj02 = AMapKit.wgs84ToGcj02(curvePoint.lat, curvePoint.lon)
            var lat = wgs84ToGcj02["lat"]!!
            var lon = wgs84ToGcj02["lon"]!!
            print("[$lon,$lat],")
        }
        println("]")
        println()
        println()
        setDistance(pretreatmentPoints)

        return analysisTracingPoint(pretreatmentPoints, reference.turningPoints)
    }

    /**
     *
     * @param carPoints:经过时间排序后的公交定位点
     * @param result   :通过了预判车辆所属的线路算法返回的结果集
     */
    @Deprecated("api已过时", level = DeprecationLevel.WARNING)
    fun analysis(carPoints: ArrayList<CarPoint>, result: Result) {
        //分组
        var positivePoint: ArrayList<CarPoint> = ArrayList(1000)
        var reversePoint: ArrayList<CarPoint> = ArrayList(1000)

        with(result) {
            //正向的线路
            var positiveLine = lines[LineDirection.POSITIVE]
            //保存班次与线路ID的映射关系
            var shuttleBusAndLineID = TreeMap<Int, Int>()

            firstStation.lat = positiveLine?.stations?.first()?.lat!!
            firstStation.lon = positiveLine.stations.first().lon
            firstStation.nextStation = positiveLine.stations.first()
            firstStation.isStation = true

            lastStation.lat = positiveLine.stations.last().lat
            lastStation.lon = positiveLine.stations.last().lon
            lastStation.nextStation = positiveLine.stations.last()
            lastStation.isStation = true

            //分组
            var carPointGroups = division(carPoints, result, shuttleBusAndLineID)

            /**
             * **************************************************************************
             * **************************************************************************
             * <title>各班次轨迹点提取与合并</title>
             * 向定位点集合插入首末站点，用来按照班次分组定位点，从出发站到目的站为一班次
             * start
             *
             */
            //形成正向反向公交站点
            val positiveReference = Reference()
            val reverseReference = Reference()
            val positiveShuttleBus = shuttleBusAndLineID.filter { it.value == positiveLine?.id }

//            positivePoint.addAll(shuttleMerge(carPointGroups.filter { positiveShuttleBus.containsKey(it.key) }.values, positiveReference))

            reversePoint.addAll(shuttleMerge(carPointGroups.filter { !positiveShuttleBus.containsKey(it.key) }.values, reverseReference))
            /**
             * <title>各班次轨迹点提取与合并</title>
             * end
             * **************************************************************************
             * **************************************************************************
             */


            /**
             * **************************************************************************
             * **************************************************************************
             * <title>轨迹分析</title>
             * 一个路线的正向反向的轨迹点分析
             * start
             *
             */
            //正向分析
//            lines[LineDirection.POSITIVE]?.points = getTracingPoints(positivePoint, positiveReference)
//            positiveReference.forEachIndexed { index, carPoint ->
//                if (positiveReferenceAngle!!.containsKey(index) && positiveReferenceAngle!![index]!! < 0.75) {
//                    positiveTurningPoints.add(carPoint)
//                }
//            }
//            insert(positiveReference, positivePoint, firstStation, -1, positiveTurningPoints, true)
//            insert(positiveReference, positivePoint, lastStation, -1, positiveTurningPoints, true)
//            positivePoint = ArrayList(positivePoint.subList(positivePoint.indexOf(firstStation), positivePoint.size))
//            setDistance(positivePoint)
//            lines[LineDirection.POSITIVE]?.points = analysisTracingPoint(positivePoint, positiveTurningPoints)

            //反向分析
            lines[LineDirection.REVERSE]?.points = getTracingPoints(reversePoint, reverseReference)
            /**
             *
             * <title>轨迹分析</title>
             * end
             * **************************************************************************
             * **************************************************************************
             */
        }

        linearResults.clear()
    }

    /**
     * 设置参考类别,两个拐点之间的的线段上的轨迹点是同一类参考类别
     *
     * @param reference
     */
    private fun setReferenceCategory(reference: ArrayList<CarPoint>, compatibilityAngle: Double): HashMap<Int, Double> {
        var category = 1.0
        var end: Int
        var aPoint: CarPoint    //a点
        var bPoint: CarPoint    //b点
        var a: Double           //三角形的a边
        var b: Double           //b边
        var c: Double           //c边
        var cAngle: Double      //c角
//        var distance: Double

        var referenceAngle = HashMap<Int, Double>()

        reference.forEachIndexed { index, carPoint ->
            //            distance = 0.0
            carPoint.category = category
            if (index > 0 && index < reference.size - 1) {
                end = index + 1
                aPoint = reference[index - 1]

                bPoint = reference[end]
                a = ToolKit.getDistance(bPoint.lat, bPoint.lon, carPoint.lat, carPoint.lon)
                b = ToolKit.getDistance(aPoint.lat, aPoint.lon, carPoint.lat, carPoint.lon)
                c = ToolKit.getDistance(aPoint.lat, aPoint.lon, bPoint.lat, bPoint.lon)

                cAngle = ToolKit.getAngleByCosineTheorem(a, b, c, ToolKit.Angle.c)
//                distance += ToolKit.getDistance(reference[end - 1].lat, reference[end - 1].lon, bPoint.lat, bPoint.lon)
//
//                while (distance < 5 || end < reference.size - 1) {
//                    end++
//                    bPoint = reference[end]
//                    a = ToolKit.getDistance(bPoint.lat, bPoint.lon, carPoint.lat, carPoint.lon)
//                    b = ToolKit.getDistance(aPoint.lat, aPoint.lon, carPoint.lat, carPoint.lon)
//                    c = ToolKit.getDistance(aPoint.lat, aPoint.lon, bPoint.lat, bPoint.lon)
//
//                    cAngle = ToolKit.getAngleByCosineTheorem(a, b, c, ToolKit.Angle.c)
//                    distance += ToolKit.getDistance(reference[end - 1].lat, reference[end - 1].lon, bPoint.lat, bPoint.lon)
//                }

                referenceAngle[index] = cAngle
                if (cAngle < compatibilityAngle) {
                    category++
                }
            }
        }

        return referenceAngle
    }

    /**
     * 插值法排序
     *
     * @param master
     * @param slave
     * @param turningPoints
     */
    @Deprecated(message = "弃用的方式")
    private fun insertSortOfCarPoint(reference: ArrayList<CarPoint>?, master: ArrayList<CarPoint>, slave: ArrayList<CarPoint>, turningPoints: ArrayList<CarPoint>, isStations: Boolean = false) {
        reference ?: return
        slave.removeAt(0)
        slave.removeAt(slave.size - 1)

        var prePointIndex = -1
        slave.forEach { sCarPoint ->
            prePointIndex = insert(reference, master, sCarPoint, prePointIndex, turningPoints, isStations)
        }
    }

    /**
     * 单点插入
     */
    @Deprecated(message = "弃用的方式")
    fun insert(reference: ArrayList<CarPoint>, master: ArrayList<CarPoint>, carPoint: CarPoint, prePointIndex: Int, turningPoints: ArrayList<CarPoint>, isStations: Boolean = false): Int {
        var minDistancePointIndex: Int
        var minDistance: Double
        var currentDistance: Double
        var start = 0
        var end = 0
        var minDistanceMap = TreeMap<Int, CarPoint>()
        var a: Double
        var b: Double
        var c: Double
        var aPoint: CarPoint
        var bPoint: CarPoint
        var insertIndex = -1
        var travelTime = 0L
        var travelDistance = 0.0
        minDistancePointIndex = 0

        var minDistancePointIndexOfRef = findMinDistancePoint(reference, minDistanceMap, carPoint)

        if (minDistancePointIndexOfRef == reference.size - 1) { //  如果定位点正好最后一个点，就直接插入到最后一点前面
            start = master.indexOf(reference[minDistancePointIndexOfRef - 1])
            end = master.indexOf(reference[minDistancePointIndexOfRef])

            //获取行驶路程跟行驶时间，用于计算速度
            travelTime = reference[minDistancePointIndexOfRef].time - reference[minDistancePointIndexOfRef - 1].time
            travelDistance = ToolKit.getDistance(carPoint.lat, carPoint.lon, reference[minDistancePointIndexOfRef].lat, reference[minDistancePointIndexOfRef].lon)
            travelDistance += ToolKit.getDistance(carPoint.lat, carPoint.lon, reference[minDistancePointIndexOfRef - 1].lat, reference[minDistancePointIndexOfRef - 1].lon)
        } else {
            preOrNext(reference, carPoint, minDistancePointIndexOfRef,
                    {
                        start = master.indexOf(reference[minDistancePointIndexOfRef - 1])
                        end = master.indexOf(reference[minDistancePointIndexOfRef])

                        //获取行驶路程跟行驶时间，用于计算速度
                        travelTime = reference[minDistancePointIndexOfRef].time - reference[minDistancePointIndexOfRef - 1].time
                        travelDistance = ToolKit.getDistance(carPoint.lat, carPoint.lon, reference[minDistancePointIndexOfRef].lat, reference[minDistancePointIndexOfRef].lon)
                        travelDistance += ToolKit.getDistance(carPoint.lat, carPoint.lon, reference[minDistancePointIndexOfRef - 1].lat, reference[minDistancePointIndexOfRef - 1].lon)
                    },
                    {
                        start = master.indexOf(reference[minDistancePointIndexOfRef])
                        end = master.indexOf(reference[minDistancePointIndexOfRef + 1])

                        //获取行驶路程跟行驶时间，用于计算速度
                        travelTime = reference[minDistancePointIndexOfRef + 1].time - reference[minDistancePointIndexOfRef].time
                        travelDistance = ToolKit.getDistance(carPoint.lat, carPoint.lon, reference[minDistancePointIndexOfRef].lat, reference[minDistancePointIndexOfRef].lon)
                        travelDistance += ToolKit.getDistance(carPoint.lat, carPoint.lon, reference[minDistancePointIndexOfRef + 1].lat, reference[minDistancePointIndexOfRef + 1].lon)
                    }, true)
        }

        //每秒行驶小于20米才符合
        if (isStations || travelDistance / Math.round(travelTime.toDouble() / 1000) < 20) {
            if (end - start <= 1) {
                insertIndex = end
            } else {
                minDistance = 10000.0
                for (i in (start + 1)..(end - 1)) {
                    currentDistance = ToolKit.getDistance(carPoint.lat, carPoint.lon, master[i].lat, master[i].lon)
                    if (minDistance > currentDistance) {
                        minDistance = currentDistance
                        minDistancePointIndex = i
                    }
                }
                if (minDistancePointIndex == master.size - 1) {
                    master.add(minDistancePointIndex, carPoint)
                    insertIndex = minDistancePointIndex
                } else {
                    preOrNext(master, carPoint, minDistancePointIndex,
                            { insertIndex = minDistancePointIndex },
                            { insertIndex = minDistancePointIndex + 1 }, false)
                }
            }
            carPoint.category = master[insertIndex].category
            master.add(insertIndex, carPoint)
//            if (insertIndex >= 0) {
//                aPoint = master[insertIndex - 1]
//                bPoint = master[insertIndex]
//                a = ToolKit.getDistance(bPoint.lat, bPoint.lon, carPoint.lat, carPoint.lon)
//                b = ToolKit.getDistance(aPoint.lat, aPoint.lon, carPoint.lat, carPoint.lon)
//                c = ToolKit.getDistance(aPoint.lat, aPoint.lon, bPoint.lat, bPoint.lon)
//                var isBelongTurning = false
//
////                如果定位点距离拐点20米以内，判断该定位点属于转弯路段
//                turningPoints.forEach {
//                    if (ToolKit.getDistance(it.lat, it.lon, carPoint.lat, carPoint.lon) < 20) {
//                        isBelongTurning = true
//                    }
//                }
//
//                if (isBelongTurning) {   // 转弯路段，可直接插入
//                    master.add(insertIndex, carPoint)
//                } else {                // 直线路段，如果插入的角大于45°，才可插入
//                    if (carPoint.isStation || ToolKit.getAngleByCosineTheorem(a, b, c, ToolKit.Angle.c) > 0.25) {
//                        master.add(insertIndex, carPoint)
//                    }
//                }
//            }
        }

        minDistanceMap.clear()
//        insertIndex = -1
        return insertIndex
    }

    fun insertStation(points: ArrayList<Point>, pointOfStation: Point) {
        val minBy = points.indexOf(points.minBy { ToolKit.getDistance(it.lat, it.lon, pointOfStation.lat, pointOfStation.lon) })
        val index = when {
            minBy <= 0 ->
                1
            minBy >= points.size - 1 ->
                points.size
            else ->
                if (ToolKit.getDistance(points[minBy - 1].lat, points[minBy - 1].lon, pointOfStation.lat, pointOfStation.lon)
                        < ToolKit.getDistance(points[minBy + 1].lat, points[minBy + 1].lon, pointOfStation.lat, pointOfStation.lon)){
                    minBy - 1
                } else
                    minBy
        }

        points.add(index, pointOfStation)
    }

    /**
     * 获取最符合的点
     */
    private fun findMinDistancePoint(points: ArrayList<CarPoint>, minDistanceMap: TreeMap<Int, CarPoint>, carPoint: CarPoint): Int {
        var minDistancePointIndexOfRef = 0
        var currentDistance: Double
        var minDistance = 10000.0
        points?.forEachIndexed { mIndex, mCarPoint ->
            if (mIndex != 0) {
                currentDistance = ToolKit.getDistance(carPoint.lat, carPoint.lon, mCarPoint.lat, mCarPoint.lon)
                if (minDistance > currentDistance) {
                    minDistance = currentDistance
                    minDistancePointIndexOfRef = mIndex
                }
                if (currentDistance <= 500) {
                    mCarPoint.distance = currentDistance
                    minDistanceMap.put(mIndex, mCarPoint)
                }
            }
        }

        /**
         * **************************************************************************
         * **************************************************************************
         * 查看是否有必要执行线性可分svm分类算法
         * start
         *
         */
        var category = -1.0
        var isExecuteSvm = false
        minDistanceMap.values.forEach {
            if (category == -1.0) {
                category = it.category
            } else if (category != it.category) {
                isExecuteSvm = true
            }
        }
        if (isExecuteSvm) {
            val carPoint = SvmTool.predict(ArrayList(minDistanceMap.values), carPoint).minBy(CarPoint::distance)
            minDistanceMap.forEach { k, v ->
                if (v == carPoint) {
                    minDistancePointIndexOfRef = k
                }
            }
        }
        /**
         *
         * end
         * **************************************************************************
         * **************************************************************************
         */
        return minDistancePointIndexOfRef
    }

    /**
     * 判断某个点是放在列表最近距离的点的前面还是后面
     * @param arrayList 列表
     * @param sCarPoint 要判断的点
     * @param index
     * @param preFun    判断如果点在前面执行的函数
     * @param nextFun   判断如果点在后面执行的函数
     */
    private fun preOrNext(arrayList: ArrayList<CarPoint>, sCarPoint: CarPoint, index: Int, preFun: () -> Unit, nextFun: () -> Unit, hasLinearResultCache: Boolean) {
        val linearResult = if (hasLinearResultCache && linearResults.containsKey(index)) {
            linearResults[index]
        } else {
            ToolKit.getDividingLine(arrayList[index].lat, arrayList[index].lon,
                    arrayList[index - 1].lat, arrayList[index - 1].lon, arrayList[index + 1].lat, arrayList[index + 1].lon)
        }

        val pointPosition = sCarPoint.lon < sCarPoint.lat * linearResult?.coefficients!! + linearResult.constant
        val prePointPosition = arrayList[index - 1].lon < arrayList[index - 1].lat * linearResult.coefficients + linearResult.constant
        if (pointPosition == prePointPosition) {
            preFun()
        } else {
            nextFun()
        }
    }

    /**
     * 分析轨迹点
     *
     * @param carPoints 公交定位点
     * @return 返回轨迹点
     */
    private fun analysisTracingPoint(carPoints: List<CarPoint>, turningPoints: ArrayList<CarPoint>): ArrayList<CarPoint> {
        val tracingPoint = ArrayList<CarPoint>()
        var startPoint = 0
        var endPoint = 2
        var lats: ArrayList<Double> = ArrayList()
        var lngs: ArrayList<Double> = ArrayList()
//        var maxLat = 0.0
//        var minLat = 1000.0

        var linearResult: LinearResult?  //线性分析结果
//        var preE: Long //前者的平均误差
        var preEndPoint = endPoint
        var preCarPoint: CarPoint? = null
        val firstPoint = carPoints.first()

        //添加起始站点
        tracingPoint.add(firstPoint)
        var carPointSubList: List<CarPoint>

        while (endPoint <= carPoints.size) {
            carPointSubList = carPoints.subList(startPoint, endPoint)

            //提取经纬度
            carPointSubList.forEach {
                if (preCarPoint != null
                        && ToolKit.getDistance(firstPoint.lat, firstPoint.lon, it.lat, it.lon) > 50
                        && ToolKit.getDistance(preCarPoint?.lat!!, preCarPoint?.lon!!, it.lat, it.lon) > 1) {

//                    if (it.lat > maxLat) {
//                        maxLat = it.lat
//                    }
//                    if (it.lat < minLat) {
//                        minLat = it.lat
//                    }

                    lats.add(it.lat)
                    lngs.add(it.lon)
                }
                preCarPoint = it
            }

            var allowableError = Consts.BUS_LINE_AVERAGE_LINE_ERROR.toDouble() / 100
            var allElementOrTwoPoint = true
            carPointSubList.forEach { carPoint ->
                turningPoints.forEach {
                    if (ToolKit.getDistance(it.lat, it.lon, carPoint.lat, carPoint.lon) < 50) {
                        allowableError = Consts.BUS_LINE_AVERAGE_ANGLE_ERROR.toDouble() / 100
                        allElementOrTwoPoint = false
                    }
                }
            }

            //进行线性分析
            linearResult = LinearAnalysisKit.linearAnalysis(lats, lngs, allElementOrTwoPoint)
            if (linearResult != null) {
                with(linearResult) {
                    if (carPoints[preEndPoint].isStation || e > allowableError
                            || carPointSubList.last().distance - carPointSubList.first().distance > Consts.BUS_TRACING_POINT_MAX_DISTANCE
                            ) {
                        startPoint = endPoint
//                        val carPointEnd = CarPoint()
//                        if (carPointSubList.first().lat < carPointSubList.last().lat) {
//                            carPointEnd.lat = maxLat
//                            carPointEnd.lon = maxLat * coefficients + constant
//                        } else {
//                            carPointEnd.lat = minLat
//                            carPointEnd.lon = minLat * coefficients + constant
//                        }
                        tracingPoint.add(carPoints[preEndPoint])
                        lats.clear()
                        lngs.clear()
//                        maxLat = 0.0
//                        minLat = 1000.0
                    }
                }
            }
            preEndPoint = endPoint
            endPoint++
        }

        //添加结束点——终点
        tracingPoint.add(carPoints.last())

        return tracingPoint
    }

    /**
     * 为每个公交定位点设置里程
     * @param list       排序后定位列表
     * @param startIndex 起始点
     */
    private fun setDistance(list: List<CarPoint>, startIndex: Int = 0) {
        var preLat = 0.0
        var preLon = 0.0
        var distance: Double
        var allDistance = 0.0

        if (startIndex != 0) {
            allDistance += list[startIndex].distance
        }

        if (startIndex >= list.size || startIndex < 0) {
            return
        }

        for (car in list.subList(startIndex, list.size)) {
            val lat = car.lat
            val lon = car.lon
            if (preLat != 0.0 && preLon != 0.0) {
                distance = ToolKit.getDistance(lat, lon, preLat, preLon)
                allDistance += distance
            }

            car.distance = allDistance
            preLat = lat
            preLon = lon
        }
    }

    /**
     * 对ArrayList的拓展方法，针对站点列表
     */
    private fun <E> ArrayList<E>.indexByNameOf(e: E): Int {
        if (e == null) {
            (0..(size - 1))
                    .filter { this[it] == null }
                    .forEach { return it }
        } else {
            (0..(size - 1))
                    .filter { i -> (e as Station).name == (this[i] as Station).name }
                    .forEach { return it }
        }
        return -1
    }
}
