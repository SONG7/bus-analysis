package com.uu.hadoop.bean

import java.io.Serializable
import java.util.ArrayList

/**
 * 线路bean
 */
data class Line(
        /**
         * 线路的ID
         */
        var id: Int = 0,
        /**
         * 线路的名称
         */
        var name: String = "",
        /**
         * 线路经过的站点，有序的
         */
        val stations: ArrayList<Station> = ArrayList(),
        /**
         * 轨迹点
         */
        var points: ArrayList<CarPoint>? = null
) : Serializable