package com.uu.common.constant;

import com.jfinal.kit.PropKit;

public interface Consts {
    /**
     * jedis缓存名称
     */
    String JEDIS_UUAPP = PropKit.use("consts.properties").get("jedis.uuapp");
    /**
     * jedis公交车数据包缓存目录名称
     */
    String JEDIS_UUAPP_BUS_PACKET = PropKit.use("consts.properties").get("jedis.uuapp.bus.packet");

    /**
     * 平均误差
     */
    long BUS_LINE_AVERAGE_LINE_ERROR = PropKit.use("consts.properties").getLong("bus.line.average.line.error");

    long BUS_LINE_AVERAGE_ANGLE_ERROR = PropKit.use("consts.properties").getLong("bus.line.average.angle.error");

    int BUS_TRACING_POINT_MAX_DISTANCE = PropKit.use("consts.properties").getInt("bus.tracing.point.max.distance", 300);

    String HADOOP_URL = PropKit.use("consts.properties").get("hadoop.url");

    String MAP_REDUCE_INPUT_PATH = PropKit.use("consts.properties").get("map.reduce.input");

    String SHUTTLE_REDUCE_OUTPUT_PATH = PropKit.use("consts.properties").get("shuttle.reduce.output");

    String TRACING_POINT_OUTPUT_PATH = PropKit.use("consts.properties").get("tracing.point.result.output");

    String CAR_LINE_RESULT_OUTPUT_PATH = PropKit.use("consts.properties").get("car.line.result.output");
    //local.analysis.source.input
    String LOCAL_SOURCE_INPUT_PATH = PropKit.use("consts.properties").get("local.analysis.source.input");
}
