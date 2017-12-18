package com.uu.hadoop.line

import com.uu.hadoop.bean.CarPoint
import com.uu.hadoop.bean.LineDirection
import com.uu.hadoop.bean.Result
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Reducer
import java.util.*

/**
 * Created by song27 on 2017/11/25 0025.
 */
class LineAnalyzerReduce : Reducer<Text, Text, Text, Text>() {
    /**
     * 只进行车辆分类（即车辆属于哪一路线），不进行路线的轨迹分析的reduce任务
     */
    override fun reduce(key: Text, values: MutableIterable<Text>, context: Context) {
        val reduce = { result:Result?, _:ArrayList<CarPoint> ->
            context.write(Text("$key"), Text("${result?.lines!![LineDirection.POSITIVE]?.name}"))
        }
        MapReduceWorker.lineAnalyzer(values, reduce)
    }
}