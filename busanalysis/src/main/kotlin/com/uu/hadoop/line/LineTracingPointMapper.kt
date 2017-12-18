package com.uu.hadoop.line

import org.apache.hadoop.io.LongWritable
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Mapper
import java.io.IOException

/**
 * Created by song27 on 2017/8/12 0012.
 */
class LineTracingPointMapper : Mapper<LongWritable, Text, Text, Text>() {

    @Throws(IOException::class, InterruptedException::class)
    override fun map(key: LongWritable, value: Text, context: Context) {
        var carInfo = value.toString().split("\t")
        if (carInfo.size != 2){
            return
        }
        var lineID = carInfo[0]
        var data = carInfo[1]
        context.write(Text(lineID), Text(data))
    }
}