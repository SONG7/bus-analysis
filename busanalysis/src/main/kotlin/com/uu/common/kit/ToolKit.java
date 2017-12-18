package com.uu.common.kit;

import com.uu.hadoop.bean.LinearResult;

public class ToolKit {

    /**
     * 获取两点之间的距离（米）
     */
    public static final double getDistance(double lat1, double lon1, double lat2, double lon2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lon1) - rad(lon2);

        double s = 2.0 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2.0), 2.0)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2.0), 2.0)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000.0) / 10000.0;
        return s;
    }

    /**
     * 地球半径
     */
    private static final double EARTH_RADIUS = 6378137.0;

    private static final double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * 三角形的3个角
     */
    public enum Angle {
        a, b, c
    }

    /**
     * 已知3条边长，求一个角的度数，a、b、c三条边的参数要严格区分好，否则影响选择获取角的角度有偏差
     *
     * @param a     边长a
     * @param b     边长b
     * @param c     变成c
     * @param angle 某个角（可以选a、b、c，角a是边a所对的角，b跟c也一样）
     * @return 返回角度，单位是π，比如计算返回的角度是90°，也就是π/2，那么返回二分之一，就是0.5
     */
    public static final double getAngleByCosineTheorem(double a, double b, double c, Angle angle) {
        double frequency = 0;
        switch (angle) {
            case a:
                frequency = Math.acos((Math.pow(b, 2.0) + Math.pow(c, 2.0) - Math.pow(a, 2.0)) / (2 * b * c));
                break;
            case b:
                frequency = Math.acos((Math.pow(a, 2.0) + Math.pow(c, 2.0) - Math.pow(b, 2.0)) / (2 * a * c));
                break;
            case c:
                frequency = Math.acos((Math.pow(a, 2.0) + Math.pow(b, 2.0) - Math.pow(c, 2.0)) / (2 * a * b));
                break;
        }
        return frequency / Math.PI;
    }

    /**
     * 求角平分线的方程
     *
     * @param middleLat 角所在的点的纬度
     * @param middleLon 角所在的点的经度
     * @param preLat    前一个点的纬度
     * @param preLon    前一个点的经度
     * @param nextLat   后（另）一个点的纬度
     * @param nextLon   后（另）一个点的经度
     * @return
     */
    public static LinearResult getDividingLine(double middleLat, double middleLon, double preLat, double preLon, double nextLat, double nextLon) {
        LinearResult linearResult = new LinearResult();
        double preSide = Math.sqrt(Math.pow(middleLat - preLat, 2.0) + Math.pow(middleLon - preLon, 2.0));
        double nextSide = Math.sqrt(Math.pow(middleLat - nextLat, 2.0) + Math.pow(middleLon - nextLon, 2.0));
        double pi = preSide / nextSide;
        double lat = (preLat + pi * nextLat) / (1 + pi);
        double lon = (preLon + pi * nextLon) / (1 + pi);

        linearResult.setCoefficients((lon - middleLon) / (lat - middleLat));
        linearResult.setConstant(lon - linearResult.getCoefficients() * lat);
        return linearResult;
    }
}
