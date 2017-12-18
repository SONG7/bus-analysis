package com.uu.common.kit

import com.uu.hadoop.bean.CarPoint
import weka.clusterers.SimpleKMeans
import weka.core.Attribute
import weka.core.DenseInstance
import weka.core.Instances
import weka.filters.unsupervised.attribute.Normalize

/**
 * Created by song27 on 2017/10/19 0019.
 */
object WekaMLKit {
    private var dataSet = Instances("points", arrayListOf(Attribute("lat"), Attribute("lon")), 100)
    private val kMeans = SimpleKMeans()
    private var latMin:Double = 0.0
    private var lonMin:Double = 0.0
    private var latMax:Double = 0.0
    private var lonMax:Double = 0.0

    /**
     * 对轨迹数据进行均值聚类
     */
    fun tracingPointClustering(carPoints: ArrayList<CarPoint>): List<CarPoint> {

        if (carPoints.size < 2){
            return emptyList()
        }

        carPoints.forEach {
            dataSet.add(DenseInstance(1.0, doubleArrayOf(it.lat, it.lon)))
        }
        latMin = carPoints.minBy { it.lat }?.lat!!
        lonMin = carPoints.minBy { it.lon }?.lon!!
        latMax = carPoints.maxBy { it.lat }?.lat!!
        lonMax = carPoints.maxBy { it.lon }?.lon!!

        clusteringByJson(dataSet, 1)
        val centroids = kMeans.clusterCentroids

        var map = ArrayList<Int>()
        dataSet.forEach {
            if (!map.contains(kMeans.clusterInstance(it))){
                map.add(kMeans.clusterInstance(it))
            }
        }

        (0 until centroids.numInstances()).forEach {
            val indexOf = map.indexOf(kMeans.clusterInstance(centroids.instance(it)))
            carPoints[indexOf].lat = centroids.instance(it).value(0) * (latMax - latMin) + latMin
            carPoints[indexOf].lon = centroids.instance(it).value(1) * (lonMax - lonMin) + lonMin
        }
        dataSet.clear()
        return carPoints.subList(0, centroids.numInstances())
    }

    fun clusteringByJson(data: Instances, numClusters:Int): SimpleKMeans {

        kMeans.numClusters = numClusters

        val normalize = Normalize()
        normalize.scale = 1.0
        normalize.translation = 0.0
        normalize.setInputFormat(data)
        data.forEach {
            normalize.input(it)
        }
        data.clear()
        normalize.batchFinished()
        while (normalize.numPendingOutput() > 0){
            data.add(normalize.output())
        }
        kMeans.buildClusterer(data)
        return kMeans
    }
}