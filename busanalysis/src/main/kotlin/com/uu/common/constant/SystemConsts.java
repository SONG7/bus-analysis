package com.uu.common.constant;

import com.jfinal.kit.PathKit;
import com.jfinal.kit.PropKit;

public interface SystemConsts {
    
    String WEB_ROOT = PathKit.getWebRootPath();
    
    /**
     * Jfinal settings
     */
    /**
     * Druid settings
     */
    static String  DRUID_DATA0_NAME = PropKit.use("druid.properties").get("druid.data0.name");
    static String  DRUID_DATA0_DRIVER_CLASS_NAME = PropKit.use("druid.properties").get("druid.data0.driver.class.name");
    static String  DRUID_DATA0_URL = PropKit.use("druid.properties").get("druid.data0.url");
    static String  DRUID_DATA0_USERNAME = PropKit.use("druid.properties").get("druid.data0.username");
    static String  DRUID_DATA0_PASSWORD = PropKit.use("druid.properties").get("druid.data0.password");
    static int     DRUID_DATA0_INITIALSIZE = PropKit.use("druid.properties").getInt("druid.data0.initialsize");
    static int     DRUID_DATA0_MINIDLE = PropKit.use("druid.properties").getInt("druid.data0.minidle");
    static int     DRUID_DATA0_MAXACTIVE = PropKit.use("druid.properties").getInt("druid.data0.maxactive");
    static int     DRUID_DATA0_MAXWAIT = PropKit.use("druid.properties").getInt("druid.data0.maxwait");
    static int     DRUID_DATA0_MAXOPENPREPAREDSTATEMENTS = PropKit.use("druid.properties").getInt("druid.data0.maxopenpreparedstatements");
    static String  DRUID_DATA0_VALIDATIONQUERY = PropKit.use("druid.properties").get("druid.data0.validationquery");
    static boolean DRUID_DATA0_TESTONBORROW = PropKit.use("druid.properties").getBoolean("druid.data0.testonborrow");
    static boolean DRUID_DATA0_TESTONRETURN = PropKit.use("druid.properties").getBoolean("druid.data0.testonreturn");
    static boolean DRUID_DATA0_TESTWHILEIDLE = PropKit.use("druid.properties").getBoolean("druid.data0.testwhileidle");
    static long    DRUID_DATA0_TIMEBETWEENEVICTIONRUNSMILLIS = PropKit.use("druid.properties").getLong("druid.data0.timebetweenevictionrunsmillis");
    
    /**
     * Jedis settings
     */
    static String JEDIS_HOSTS       = PropKit.use("system.properties").get("jedis.hosts");
    static String JEDIS_PORTS       = PropKit.use("system.properties").get("jedis.ports");
    static String JEDIS_PASSWDS     = PropKit.use("system.properties").get("jedis.passwds");
    static String JEDIS_CACHENAMES  = PropKit.use("system.properties").get("jedis.cachenames");

    /**
     * Hbase settings
     */
    static String HBASE_PORT        = PropKit.use("system.properties").get("hbase.port");
    static String HBASE_HOSTS       = PropKit.use("system.properties").get("hbase.hosts");
}
