package com.uu.common.kit

import com.uu.hadoop.bean.CarPoint
import com.uu.hadoop.bean.Point
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints

/**
 * Created by song27 on 2017/11/30 0030.
 */
object CurveFitKit {

    private fun polynomial(carPoints: ArrayList<CarPoint>): DoubleArray? {
        val obs = WeightedObservedPoints()

        carPoints.forEach {
            obs.add(it.lat, it.lon)
        }
        // Instantiate a third-degree polynomial fitter.
        val fitter = PolynomialCurveFitter.create(2)

        // Retrieve fitted parameters (coefficients of the polynomial function).
        return fitter.fit(obs.toList())
    }

    /**
     * 多项式曲线拟合
     */
    fun getPointsByPolynomial(carPoints: ArrayList<CarPoint>) {
        val latMinBy = carPoints.minBy { it.lat }
        val latMaxBy = carPoints.maxBy { it.lat }

        val coefficients = polynomial(carPoints)
        var lat = latMinBy?.lat!!// * 10000000
        var lon = 0.0
        carPoints.clear()

        while (lat < latMaxBy?.lat!!) {
            coefficients?.forEachIndexed { index, d ->
                lon += d * Math.pow(lat, index.toDouble())
            }
//            lon = coefficients!![0] + coefficients[1] * lat + coefficients[2] * Math.pow(lat, 2.0) + coefficients[3] * Math.pow(lat, 3.0)
            var carPoint = CarPoint()
            carPoint.lon = lon
            carPoint.lat = lat
            carPoints.add(carPoint)
            lat += 0.00005
            lon = 0.0
        }
    }

    private fun convert(i: Int, t: Double): Double {
        when (i) {
            -2 -> return (((-t + 3) * t - 3) * t + 1) / 6
            -1 -> return ((3 * t - 6) * t * t + 4) / 6
            0 -> return (((-3 * t + 3) * t + 3) * t + 1) / 6
            1 -> return t * t * t / 6
        }
        return 0.0
    }

    /**
     * B样条曲线拟合,获得准道路中心线
     */
    fun getBSplineCurvePoints(lats: DoubleArray, lons: DoubleArray, numPts: Int, steps: Int): ArrayList<Point> {
        val pts = numPts * steps + 1
        val curve = ArrayList<Point>(pts)
//        curve.add(point(2, 0.0, lats, lons))
        for (i in 2 until numPts) {
            (1..steps).mapTo(curve) {
                point(i, it / steps.toDouble(), lats, lons)
            }
        }
        return curve
    }

    /**
     * Evaluate an ith point on the B spline
     *
     * @param i     第几个点
     * @param t     时间，取值范围: 0 <= t <= 1
     * @param lats
     * @param lons
     * @return
     */
    private fun point(i: Int, t: Double, lats: DoubleArray, lons: DoubleArray): Point {
        var px = 0.0
        var py = 0.0
        for (j in -2..1) {
            px += convert(j, t) * lats[i + j]
            py += convert(j, t) * lons[i + j]
        }
        return Point(px, py)
    }
}