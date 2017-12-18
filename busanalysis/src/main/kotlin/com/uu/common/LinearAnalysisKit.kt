package com.uu.common

import com.uu.common.exception.DataFrameNotAlignedException
import com.uu.common.kit.ToolKit
import com.uu.hadoop.bean.LinearResult

/**
 * Created by song27 on 2017/8/23 0023.
 */
object LinearAnalysisKit {

    /**
     * 公交定位点线性回归分析
     *
     * @param lats:纬度集合
     * @param lons:经度集合
     * @return 返回平均误差（单位是米）
     */
    fun linearAnalysis(lats: List<Double>, lons: List<Double>, allElementOrTwoPoint:Boolean = false) : LinearResult? {
        if (lats.size != lons.size){
            throw DataFrameNotAlignedException("The data frame is not aligned!")
        }
        if (lats.size <= 1 || lons.size <= 1){
            return null
        }
        var linearResult = LinearResult()

        if (allElementOrTwoPoint){
            linearAnalysisByAllElement(lats, lons, linearResult)
        } else
            linearAnalysisByTwoPoint(lats, lons, linearResult)

        if (linearResult.coefficients == Double.NaN || linearResult.constant == Double.NaN){
            return null
        }
        return linearResult
    }

    private fun linearAnalysisByAllElement(lats: List<Double>, lons: List<Double>, linearResult: LinearResult){
        var latAvg = lats.average()
        var lonAvg = lons.average()
        var sum = 0.0
        var sum2 = 0.0
        lats.forEachIndexed { index, d ->
            sum += d * lons[index]
            sum2 += Math.pow(d, 2.0)
        }
        var coefficients = (sum - lats.size * latAvg * lonAvg) / (sum2 - lats.size * Math.pow(latAvg, 2.0))
        var constant = lonAvg - latAvg * coefficients
        var residuals = ArrayList<Double>()
        var lonPre:Double

        lats.forEachIndexed { index, lat ->
            lonPre = coefficients * lat + constant
            residuals.add(ToolKit.getDistance(lat, lons[index], lat, lonPre))
        }
        linearResult.coefficients = coefficients
        linearResult.constant = constant
        linearResult.e = residuals.average()
    }

    private fun linearAnalysisByTwoPoint(lats: List<Double>, lons: List<Double>, linearResult: LinearResult){
        var residuals = ArrayList<Double>()
        linearResult.coefficients = (lons.last() - lons.first()) / (lats.last() - lats.first())
        linearResult.constant = lons.last() - linearResult.coefficients * lats.last()
        var lonPre:Double
        lats.forEachIndexed { index, lat ->
            lonPre = linearResult.coefficients * lat + linearResult.constant
            residuals.add(ToolKit.getDistance(lat, lons[index], lat, lonPre))
        }
        linearResult.e = residuals.average()
    }
}