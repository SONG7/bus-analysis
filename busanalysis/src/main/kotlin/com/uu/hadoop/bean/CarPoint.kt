package com.uu.hadoop.bean

import org.apache.hadoop.io.WritableComparable
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.io.Serializable

/**
 * Created by song27 on 2017/8/11 0011.
 */
class CarPoint : WritableComparable<CarPoint>, Serializable {
    //车牌
    var carPlate: String? = null
    //进行定位的时间
    var time: Long = 0
    //WGS84坐标纬度
    var lat: Double = 0.toDouble()
    //WGS84坐标经度
    var lon: Double = 0.toDouble()
    //里程
    var distance: Double = 0.toDouble()
    //班次
    var shuttleBus: Int = 0
    //下个站点
    var nextStation: Station? = null
    //所属线路ID
    var lineID: Int? = 0
    //序号
    var position: Int? = -1
    //类别
    var category:Double = 0.0

    var isStation:Boolean = false

    var read:Boolean = false

    fun setCar(carPlate: String, time: Long, lat: Double, lon: Double) {
        this.carPlate = carPlate
        this.time = time
        this.lat = lat
        this.lon = lon
    }

    @Throws(IOException::class)
    override fun write(out: DataOutput) {
        out.writeUTF(carPlate!!)
        out.writeLong(time)
        out.writeDouble(lat)
        out.writeDouble(lon)
    }

    @Throws(IOException::class)
    override fun readFields(input: DataInput) {
        carPlate = input.readUTF()
        time = input.readLong()
        lat = input.readDouble()
        lon = input.readDouble()
    }


    override fun compareTo(o: CarPoint): Int {
        return if (time < o.time) -1 else 1
    }

    override fun toString(): String {
        return "CarPoint(carPlate=$carPlate, time=$time, lat=$lat, lon=$lon, distance=$distance, shuttleBus=$shuttleBus, nextStation=$nextStation, lineID=$lineID, position=$position, category=$category, isStation=$isStation, read=$read)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CarPoint
        if (lat != other.lat || lon != other.lon || lineID != other.lineID)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = lat.hashCode()
        result = 31 * result + lon.hashCode()
        result = 31 * result + (lineID ?: 0)
        return result
    }

}
