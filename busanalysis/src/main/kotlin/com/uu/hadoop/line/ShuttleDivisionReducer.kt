package com.uu.hadoop.line

import com.alibaba.fastjson.JSONObject
import com.uu.common.TracingPointAnalyzer
import com.uu.hadoop.bean.CarPoint
import com.uu.hadoop.bean.LineDirection
import com.uu.hadoop.bean.Result
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Reducer
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by song27 on 2017/11/24 0024.
 */
class ShuttleDivisionReducer : Reducer<Text, Text, Text, Text>() {

    /**
     * 进行车辆分类（即车辆属于哪一路线），并区分班次并合并班次，不进行路线的轨迹分析的reduce任务
     */
    override fun reduce(key: Text?, values: MutableIterable<Text>, context: Context) {

        //回调的函数
        val reduce = { result:Result?, carPoints:ArrayList<CarPoint> ->
            var shuttleBusAndLineID = TreeMap<Int, Int>()
            val carPointGroups = TracingPointAnalyzer.division(carPoints, result!!, shuttleBusAndLineID)
            val positiveShuttleBus = shuttleBusAndLineID.filter { it.value == result.lines[LineDirection.POSITIVE]?.id }
            var data = JSONObject()
            carPointGroups.forEach { shuttleBus, arrayList ->
                var lineID = result.lines[LineDirection.REVERSE]!!.id
                if (positiveShuttleBus.containsKey(shuttleBus))
                    lineID = result.lines[LineDirection.POSITIVE]!!.id

                data.put("points", arrayList)
                context.write(Text(lineID.toString()), Text(data.toJSONString()))
            }
        }

        //进行分类
        MapReduceWorker.lineAnalyzer(values, reduce)
    }
}