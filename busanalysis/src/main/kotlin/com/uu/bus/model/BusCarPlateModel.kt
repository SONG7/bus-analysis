package com.uu.bus.model

import com.jfinal.kit.StrKit
import com.jfinal.plugin.activerecord.Db
import com.jfinal.plugin.activerecord.Record

/**
 * Created by song27 on 2017/11/25 0025.
 */
object BusCarPlateModel {
    private val TABLE_NAME = "uu_bus_car_plate"
    /**
     * 获取车牌号数据
     *
     * @param fields 需要查询的字段
     * @param where  查询条件
     * @param args   参数
     * @return
     */
    fun getCarPlate(fields: String, where: String, vararg args: Any): Record {
        val sql = StringBuilder(" SELECT ")
        sql.append(fields).append(" FROM ").append(TABLE_NAME).append(" ")
        if (StrKit.isBlank(where)) {
            sql.append(" WHERE ").append(where)
        }
        sql.append(" limit 1 ")
        return Db.findFirst(sql.toString(), *args)
    }
}