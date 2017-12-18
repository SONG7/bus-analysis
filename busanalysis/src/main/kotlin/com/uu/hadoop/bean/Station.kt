package com.uu.hadoop.bean

import java.io.Serializable
import java.util.ArrayList

/**
 * 站点bean
 */
data class Station(
        /**
         * 站点名称
         */
        var name: String = "",
        /**
         * 纬度
         */
        var lat: Double = 0.0,
        /**
         * 经度
         */
        var lon: Double = 0.0,
        /**
         * 经过该站点的线路的ID集
         */
        val lines: ArrayList<Int> = ArrayList()
) : Serializable