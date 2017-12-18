package com.uu.hadoop.bean

/**
 * 封装站点跟某车辆定位数据实例
 */
data class StationNearCarPoint(var distance:Long = 0, var station:Station? = null)
