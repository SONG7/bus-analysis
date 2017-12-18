package com.uu.hadoop.line

import com.alibaba.fastjson.JSONObject
import com.uu.hadoop.bean.CarPoint
import org.apache.hadoop.io.LongWritable
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Mapper
import java.io.IOException
import java.text.SimpleDateFormat

/**
 * Created by song27 on 2017/12/13 0013.
 */
class SourceLocationPointMapper : Mapper<LongWritable, Text, Text, Text>()  {
    private val car = CarPoint()
    private val simple = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    private var preLat:Double = 0.0
    private var preLon:Double = 0.0

    @Throws(IOException::class, InterruptedException::class)
    override fun map(key: LongWritable, value: Text, context: Context) {
        var carInfo = value.toString().split(",")
        var carPlate = carInfo[3]
        var time = simple.parse(carInfo[4]).time
        var lat = carInfo[1].toDouble()
        var lon = carInfo[2].toDouble()

        if (preLat == lat && preLon == lon){
            return
        }
        car.setCar(carPlate, time, lat, lon)
        context.write(Text(carPlate), Text(JSONObject.toJSONString(car)))
        preLat = lat
        preLon = lon
    }
}