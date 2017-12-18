package com.uu.application

import com.jfinal.plugin.activerecord.Db
import com.jfinal.plugin.redis.Redis
import com.uu.common.proxy.DbBase
import com.uu.common.proxy.DbSubject
import com.uu.common.kit.ToolKit
import com.uu.common.constant.Consts
import com.uu.hadoop.bean.Line
import com.uu.hadoop.bean.Station

/**
 * Created by song27 on 2017/8/16 0016.
 *
 * 该类用于把数据库中的线路跟站点，整合重组存放到Redis中，便于分析程序
 * 获取相关线路站点信息数据。
 */
object FillDataToRedis : DbSubject {
    override fun onStart() {
        val cache = Redis.use(Consts.JEDIS_UUAPP)!!
        val stations = HashMap<String, Station>()
        val lines = HashMap<Int, Line>()
        var station: Station
        var line: Line
        var stationName: String

        for (record in Db.find("SELECT s.station_name,s.lon,s.lat,s.bus_line_id,l.line_name FROM uu_bus_stations s LEFT JOIN uu_bus_lines l ON s.bus_line_id=l.id" +
                " ORDER BY s.bus_line_id,s.station_no")) {
            stationName = record["station_name"]

            var i = 0
            var d:Long
            var d2:Long
            while (true) {
                if (stations["$i:$stationName:0"] == null){
                    station = Station()
                    station.lat = record["lat"]
                    station.lon = record["lon"]
                    station.name = record["station_name"]
                    stations["$i:$stationName:0"] = station
                    break
                } else if (Math.round(ToolKit.getDistance(record["lat"], record["lon"],
                        stations["$i:$stationName:0"]?.lat!!, stations["$i:$stationName:0"]?.lon!!)) in 11..499
                        && stations["$i:$stationName:1"] == null){
                    station = Station()
                    station.lat = record["lat"]
                    station.lon = record["lon"]
                    station.name = record["station_name"]
                    stations["$i:$stationName:1"] = station
                    break
                } else {
                    d = Math.round(ToolKit.getDistance(record["lat"], record["lon"],
                            stations["$i:$stationName:0"]?.lat!!, stations["$i:$stationName:0"]?.lon!!))
                    d2 = Math.round(ToolKit.getDistance(record["lat"], record["lon"],
                            stations["$i:$stationName:1"]?.lat?: 0.0, stations["$i:$stationName:1"]?.lon?: 0.0))
                    if (d < 1000 && d2 <1000){
                        station = if (d < d2){
                            stations["$i:$stationName:0"]!!
                        } else {
                            stations["$i:$stationName:1"]!!
                        }
                        break
                    }
                }
                i++
            }

            if (lines[record["bus_line_id"]] == null) {
                line = Line()
                line.id = record["bus_line_id"]
                line.name = record["line_name"]
                line.stations.add(station)
                lines[record["bus_line_id"]] = line
            } else {
                line = lines[record["bus_line_id"]]!!
                line.stations.add(station)
            }

            if (!station.lines.contains(line.id)) {
                station.lines.add(line.id)
            }
        }

        lines.forEach { k, v ->
            cache["line:$k"] = v
        }

        stations.forEach { k, v ->
            cache["station:$k"] = v
        }
    }

}

fun main(args:Array<String>) {
    val subject = DbBase(FillDataToRedis).proxy as DbSubject
    subject.onStart()
}
