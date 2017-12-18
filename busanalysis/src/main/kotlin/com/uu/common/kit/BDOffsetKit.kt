package com.uu.common.kit

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.squareup.okhttp.*
import com.uu.hadoop.bean.CarPoint
import java.io.IOException

/**
 * Created by song27 on 2017/10/11 0011.
 */
@Deprecated(message = "放弃了的方式")
object BDOffsetKit {

    fun getTracingPoints(startTime: Long, endTime: Long): ArrayList<CarPoint> {
        var url = "http://yingyan.baidu.com/api/v3/track/gettrack?ak=813lcyHzmhtLgirsRV7suOdvAeB33txm&service_id=151590&entity_name=test01&start_time=$startTime&end_time=$endTime&is_processed=1&process_option=need_denoise=1,need_vacuate=1,need_mapmatch=1,radius_threshold=20,transport_mode=driving&coord_type_output=gcj02"
        val client = OkHttpClient()
        val request = Request.Builder().url(url)
                .get()
                .build()
        var response: Response? = null
        var pointList = ArrayList<CarPoint>()
        try {
            response = client.newCall(request).execute()
            val body = response!!.body().string()
            println(body)
            val jsonObject: JSONObject
            jsonObject = JSON.parseObject(body)
            val points = jsonObject.getJSONArray("points")
            if (jsonObject["status"] == 0) {
                for (i in 0 until points.size) {
                    val lat = points.getJSONObject(i).getDouble("latitude")
                    val lon = points.getJSONObject(i).getDouble("longitude")
                    var carPoint = CarPoint()
                    carPoint.lon = lon
                    carPoint.lat = lat
                    pointList.add(carPoint)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return pointList
    }

    fun addPoints(carPoints: ArrayList<CarPoint>): Map<String, Long> {
        var url = "http://yingyan.baidu.com/api/v3/track/addpoints"
        val client = OkHttpClient()
//        {"entity_name": "test01","loc_time": 1483279659,"latitude": 23.056,"longitude": 113.74091666666668,"coord_type_input": "wgs84"}
        var pointList = ArrayList<JSONObject>()
        var start = (System.currentTimeMillis() - 12 * 60 * 60000) / 1000
        var end = start

        carPoints.forEach {
            end += 10
            var record = JSONObject()
            record.put("entity_name", "test01")
            record.put("loc_time", end)
            record.put("latitude", it.lat)
            record.put("longitude", it.lon)
            record.put("coord_type_input", "wgs84")
            pointList.add(record)
        }
        end += 2

        var count = pointList.size / 90
        if (pointList.size % 90 > 0) {
            count++
        }
        println("size:${pointList.size}++++++count:$count")

        var range = mapOf(Pair("start", start), Pair("end", end))
        var jsonArray = JSONArray()

        var response: Response
        val builder = Request.Builder().url(url)
        var request: Request
        var formBody: RequestBody

        for (i in 0 until count) {
            if (i == count - 1) {
                jsonArray.addAll(pointList.subList(i * 90, pointList.size))
            } else {
                jsonArray.addAll(pointList.subList(i * 90, (i + 1) * 90))
            }

            formBody = FormEncodingBuilder()
                    .add("ak", "813lcyHzmhtLgirsRV7suOdvAeB33txm")
                    .add("service_id", "151590")
                    .add("point_list", jsonArray.toJSONString())
                    .build()

            request = builder
                    .post(formBody)
                    .build()

            response = client.newCall(request).execute()

            jsonArray.clear()

//            println(response!!.body().string())
        }

        return range
    }
}