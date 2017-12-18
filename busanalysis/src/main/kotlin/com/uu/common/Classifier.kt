package com.uu.common

import com.uu.hadoop.bean.CarPoint
import com.uu.hadoop.bean.Result
import com.uu.hadoop.bean.Station

/**
 * Created by song27 on 2017/11/22 0022.
 */
interface Classifier {
    fun determineRoute(carPoints: List<CarPoint>, screenOutStations: List<Station>): Result?
}