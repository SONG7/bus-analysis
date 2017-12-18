package com.uu.hadoop.line

import com.alibaba.fastjson.JSONObject
import com.jfinal.log.Log4jLog
import com.jfinal.plugin.redis.Cache
import com.jfinal.plugin.redis.Redis
import com.uu.common.DecisionTreeClassifier
import com.uu.common.constant.Consts
import com.uu.common.exception.LineException
import com.uu.hadoop.bean.CarPoint
import com.uu.hadoop.bean.Result
import com.uu.hadoop.bean.Station
import org.apache.hadoop.io.Text
import java.util.*

/**
 * Created by song27 on 2017/11/25 0025.
 */
object MapReduceWorker {
    private val log = Log4jLog.getLog(MapReduceWorker.javaClass)
    private val cache: Cache = Redis.use(Consts.JEDIS_UUAPP)

    private val keys: Set<String>

    init {
        keys = cache.keys("station:*")
    }

    private var carPoints = ArrayList<CarPoint>()

    private val list: List<Station> = cache.mget(*keys.toTypedArray()) as List<Station>

    fun lineAnalyzer(values: MutableIterable<Text>, reduce:(result:Result?, carPoints:ArrayList<CarPoint>)->Unit){
        values.forEach {
            var carPoint = CarPoint()
            val parse = JSONObject.parseObject(it.toString())
            carPoint.carPlate = parse.getString("carPlate")
            carPoint.setCar(parse.getString("carPlate")!!, parse.getLong("time"), parse.getDouble("lat"), parse.getDouble("lon"))
            carPoints.add(carPoint)
        }

        //按时间排序
        Collections.sort(carPoints, { o1, o2 -> (o1.time - o2.time).toInt() })
        var result: Result? = null
        try {
            result = DecisionTreeClassifier.determineRoute(carPoints, list)
            reduce(result, carPoints)
        } catch (e: LineException) {
            log.error(e.message, e)
        }

        carPoints.clear()
    }
}