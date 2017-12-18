package com.uu.application

import com.alibaba.fastjson.JSONObject
import com.uu.common.kit.AMapKit
import java.io.File
import java.util.*

/**
 * Created by song27 on 2017/12/13 0013.
 *
 * 用于对json数据转换成高德地图的数据输入格式
 */
fun main(args: Array<String>) {
    val rootPath = System.getProperty("user.dir")
    val file = File("$rootPath\\src\\main\\web\\data\\wgs84-points.json")
    val test = StringBuffer()
    file.forEachLine {
        test.append(it)
    }
    val parseArray = JSONObject.parseArray(test.toString())
    Collections.sort(parseArray,
            { o1, o2 -> ((o1 as JSONObject).getLongValue("time") - (o2 as JSONObject).getLongValue("time")).toInt() })
    for (i in 0 until parseArray.size) {
        val jsonObject = parseArray.getJSONObject(i)
        val wgs84ToGcj02 = AMapKit.wgs84ToGcj02(jsonObject.getDouble("lat"), jsonObject.getDouble("lon"))
        jsonObject["lat"] = wgs84ToGcj02["lat"]!!
        jsonObject["lon"] = wgs84ToGcj02["lon"]!!
    }
    var resultFile = File("$rootPath\\src\\main\\web\\data\\tracing-points.json")
    resultFile.writeText(parseArray.toJSONString())
}