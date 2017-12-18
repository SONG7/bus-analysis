package com.uu.common.kit

import com.uu.hadoop.bean.CarPoint
import libsvm.svm
import libsvm.svm_node
import libsvm.svm_parameter
import libsvm.svm_problem

/**
 * Created by song27 on 2017/9/15 0015.
 */
object SvmTool {
    /**
     * 使用SVM算法根据训练集，判断预测点的类型
     *
     * @param points 训练集
     * @param point  预测点
     */
    fun predict(points: ArrayList<CarPoint>, point: CarPoint): List<CarPoint> {
        val data = arrayOfNulls<Array<svm_node>>(points.size) //训练集的向量表
        val labels = ArrayList<Double>()
        var minLat = points.minBy { it.lat }?.lat!!
        var minLon = points.minBy { it.lon }?.lon!!
        if (minLat > point.lat) {
            minLat = point.lat
        }
        if (minLon > point.lon) {
            minLon = point.lon
        }

        points.forEachIndexed { index, carPoint ->
            var p0 = svm_node()
            p0.index = 0
            p0.value = Math.round((carPoint.lat - minLat) * 100000).toDouble()
            var p1 = svm_node()
            p1.index = 1
            p1.value = Math.round((carPoint.lon - minLon) * 100000).toDouble()
            data[index] = arrayOf(p0, p1)
            labels.add(carPoint.category)
        }

        //定义svm_problem对象
        val problem = svm_problem()
        problem.l = data.size//向量个数
        problem.x = data //训练集向量表
        problem.y = labels.toDoubleArray() //对应的lable数组

        //定义svm_parameter对象
        val param = svm_parameter()
        param.svm_type = svm_parameter.C_SVC
        param.kernel_type = svm_parameter.LINEAR
        param.cache_size = 100.0
        param.eps = 0.00001
        param.C = 0.5

        //训练SVM分类模型
        svm.svm_set_print_string_function {}
//        println(svm.svm_check_parameter(problem, param)) //如果参数没有问题，则svm.svm_check_parameter()函数返回null,否则返回error描述。
        val model = svm.svm_train(problem, param) //svm.svm_train()训练出SVM分类模型

        //定义测试数据点c
        val pc0 = svm_node()
        pc0.index = 0
        pc0.value = Math.round((point.lat - minLat) * 100000).toDouble()
        val pc1 = svm_node()
        pc1.index = 1
        pc1.value = Math.round((point.lon - minLon) * 100000).toDouble()
        val pc = arrayOf(pc0, pc1)
        val svm_predict = svm.svm_predict(model, pc)
        return points.filter { it.category == svm_predict }
    }
}