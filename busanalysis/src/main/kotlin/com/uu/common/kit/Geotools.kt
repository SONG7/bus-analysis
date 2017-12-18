package com.uu.common.kit

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.Point
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.opengis.geometry.MismatchedDimensionException
import org.opengis.referencing.FactoryException
import org.opengis.referencing.operation.TransformException

/**
 * Created by song27 on 2017/10/18 0018.
 */
@Deprecated(message = "放弃了的方式")
object Geotools {
    @Throws(FactoryException::class, MismatchedDimensionException::class, TransformException::class)
    fun convert(lat: Double, lon: Double): Point {
        // 传入原始的经纬度坐标
        val sourceCoord = Coordinate(lon, lat)
        val geoFactory = GeometryFactory()
        val sourcePoint = geoFactory.createPoint(sourceCoord)

        // 这里是以OGC WKT形式定义的是World Mercator投影，网页地图一般使用该投影
        val strWKTMercator = ("PROJCS[\"World_Mercator\","
                + "GEOGCS[\"GCS_WGS_1984\","
                + "DATUM[\"WGS_1984\","
                + "SPHEROID[\"WGS_1984\",6378137,298.257223563]],"
                + "PRIMEM[\"Greenwich\",0],"
                + "UNIT[\"Degree\",0.017453292519943295]],"
                + "PROJECTION[\"Mercator_1SP\"],"
                + "PARAMETER[\"False_Easting\",0],"
                + "PARAMETER[\"False_Northing\",0],"
                + "PARAMETER[\"Central_Meridian\",0],"
                + "PARAMETER[\"latitude_of_origin\",0],"
                + "UNIT[\"Meter\",1]]")
        val mercatroCRS = CRS.parseWKT(strWKTMercator)
        // 做投影转换，将WCG84坐标转换成世界墨卡托投影转
        val transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, mercatroCRS)

        // 返回转换以后的X和Y坐标
        return JTS.transform(sourcePoint, transform) as Point
    }
}