package com.uu.common

import com.uu.common.kit.ToolKit
import com.uu.common.kit.WekaMLKit
import com.uu.hadoop.bean.CarPoint

/**
 * Created by song27 on 2017/10/12 0012.
 */
@Deprecated(message = "放弃了的方式")
object PointPretreatmentKit {

    fun pretreatment(points: ArrayList<CarPoint>): ArrayList<CarPoint> {
        var newPoints = ArrayList<CarPoint>(1000)

        var start = -1
        var end = -1
        var latSumBy = 0.0
        var latAgv: Double
        var lonSumBy = 0.0
        var lonAgv: Double
        var size: Int

        var tempPoints = ArrayList<CarPoint>()
        var lastCenterPoint:CarPoint
        var isClustering:Boolean

        var index = 0
        while (index < points.size) {
            if (start == -1) {
                start = index
                end = index
            } else {
                end++
                for (i in start..end) {
                    latSumBy += points[i].lat
                    lonSumBy += points[i].lon
                    tempPoints.add(points[i])
                }
                size = end + 1 - start
                latAgv = latSumBy / size
                lonAgv = lonSumBy / size

                if (newPoints.size > 0){
                    lastCenterPoint = newPoints.last()
                    isClustering = PointPretreatmentKit.isClustering(tempPoints, lastCenterPoint.lat, lastCenterPoint.lon, 20.0)
                } else {
                    isClustering = PointPretreatmentKit.isClustering(tempPoints, latAgv, lonAgv, 5.0)
                }

                if (isClustering) {
                    newPoints.addAll(WekaMLKit.tracingPointClustering(tempPoints))
                    start = -1
                }
            }

            tempPoints.clear()
            index++
            latSumBy = 0.0
            lonSumBy = 0.0
        }
        return newPoints
    }

    private fun isClustering(tempPoints:ArrayList<CarPoint>, centerLat:Double, centerLon: Double, maxDistance:Double):Boolean{
        var operate = false
        tempPoints.forEach {
            var distance = ToolKit.getDistance(it.lat, it.lon, centerLat, centerLon)
            if (distance > maxDistance){
                operate = true
                return@forEach
            }
        }
        return operate
    }
}
