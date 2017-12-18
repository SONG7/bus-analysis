package com.uu.hadoop.bean

import java.util.HashMap

/**
 * 分析返回的数据集
 */
data class Result(
        val lines: HashMap<LineDirection, Line> = HashMap(),
        val stationNearCarPoints: HashMap<Int, StationNearCarPoint> = HashMap()
)