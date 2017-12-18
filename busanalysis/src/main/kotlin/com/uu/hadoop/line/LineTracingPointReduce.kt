package com.uu.hadoop.line

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.jfinal.plugin.redis.Cache
import com.jfinal.plugin.redis.Redis
import com.uu.common.DecisionTreeClassifier
import com.uu.common.TracingPointAnalyzer
import com.uu.common.constant.Consts
import com.uu.common.kit.CurveFitKit
import com.uu.hadoop.bean.*
import org.apache.hadoop.io.IntWritable
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Reducer
import java.io.IOException

/**
 * Created by song27 on 2017/5/17 0017.
 */
class LineTracingPointReduce : Reducer<Text, Text, Text, Text>() {
    private val cache: Cache = Redis.use(Consts.JEDIS_UUAPP)
    var shuttleList = ArrayList<ArrayList<CarPoint>>()

    @Throws(IOException::class, InterruptedException::class)
    override fun reduce(key: Text, values: Iterable<Text>, context: Context) {
        var jsonArray: JSONArray
        var data: JSONObject? = null

        values.forEach {
            data = JSON.parseObject(it.toString())
            jsonArray = data!!.getJSONArray("points")
            var shuttle = ArrayList<CarPoint>()

            (0 until jsonArray.size)
                    .map { i -> jsonArray.getJSONObject(i) }
                    .forEach { json ->
                        var carPoint = CarPoint()
                        carPoint.setCar(json.getString("carPlate"), json.getLong("time"),
                                json.getDouble("lat"), json.getDouble("lon"))
                        carPoint.category = json.getDouble("category")
                        carPoint.distance = json.getDouble("distance")
                        carPoint.lineID = json.getInteger("lineID")
                        carPoint.isStation = json.getBoolean("station")
                        carPoint.position = json.getInteger("position")
                        shuttle.add(carPoint)
                    }
            shuttleList.add(shuttle)
        }
        val line:Line = cache["line:$key"]

        val tracingPoints = TracingPointAnalyzer.getTracingPointsByLocateDataSet(shuttleList, line!!)
        val lats = DoubleArray(tracingPoints.size)
        val lons = DoubleArray(tracingPoints.size)
        tracingPoints.forEachIndexed { index, carPoint ->
            lats[index] = carPoint.lat
            lons[index] = carPoint.lon
        }

        val curvePoints = CurveFitKit.getBSplineCurvePoints(lats, lons, tracingPoints.size - 1, 2)

        var firstPoint:Point? = null
        var lastPoint:Point? = null
        line.stations.forEachIndexed { index, station ->
            var point = Point(station.lat, station.lon)
            if (index == 0) firstPoint = point
            if (index == line.stations.size - 1) lastPoint = point
            TracingPointAnalyzer.insertStation(curvePoints, point)
        }

        jsonArray = JSONArray()
        jsonArray.addAll(curvePoints.subList(curvePoints.indexOf(firstPoint), curvePoints.indexOf(lastPoint)))
        context.write(Text("lineID:" + key.toString()), Text(jsonArray.toJSONString()))

        shuttleList.clear()
    }
}
