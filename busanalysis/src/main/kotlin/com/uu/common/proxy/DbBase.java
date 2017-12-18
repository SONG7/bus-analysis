package com.uu.common.proxy;


import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.wall.WallFilter;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.activerecord.dialect.MysqlDialect;
import com.jfinal.plugin.druid.DruidPlugin;
import com.jfinal.plugin.redis.RedisPlugin;
import com.uu.common.constant.SystemConsts;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DbBase implements InvocationHandler {
    protected static DruidPlugin dp;
    protected static ActiveRecordPlugin activeRecord;
    protected static RedisPlugin redisPlugin[];
    private Object subject;
    private Aop aop;

    public interface Aop{
        void before() throws Exception;
        void after() throws Exception;
    }

    public DbBase(Object subject){
        this(subject, null);
    }

    public DbBase(Object subject, Aop aop){
        this.subject = subject;
        this.aop = aop;
    }

    /**
     * 数据连接地
     */
    private static final String URL = SystemConsts.DRUID_DATA0_URL;

    /**
     * 数据库账号
     */
    private static final String USERNAME = SystemConsts.DRUID_DATA0_USERNAME;

    /**
     * 数据库密码
     */
    private static final String PASSWORD = SystemConsts.DRUID_DATA0_PASSWORD;
    /**
     * 数据库驱动
     */
    private static final String DRIVER = "com.mysql.jdbc.Driver";

    /**
     * 数据库类型（如mysql，oracle）
     */
    private static final String DATABASE_TYPE = "mysql";

    public static void before() throws Exception {
        dp = new DruidPlugin(URL, USERNAME, PASSWORD, DRIVER);

        dp.addFilter(new StatFilter());

        dp.setInitialSize(3);
        dp.setMinIdle(2);
        dp.setMaxActive(5);
        dp.setMaxWait(60000);
        dp.setTimeBetweenEvictionRunsMillis(120000);
        dp.setMinEvictableIdleTimeMillis(120000);

        WallFilter wall = new WallFilter();
        wall.setDbType(DATABASE_TYPE);
        dp.addFilter(wall);

        dp.getDataSource();
        dp.start();

        activeRecord = new ActiveRecordPlugin(dp);
        activeRecord.setDialect(new MysqlDialect())
        //.setDevMode(true)
        //.setShowSql(true)  //是否打印sql语句
        ;
        activeRecord.start();

        String[] hosts = SystemConsts.JEDIS_HOSTS.split(",");
        String[] ports = SystemConsts.JEDIS_PORTS.split(",");
        String[] names = SystemConsts.JEDIS_CACHENAMES.split(",");
        String[] passwds = SystemConsts.JEDIS_PASSWDS.split(",");
        redisPlugin = new RedisPlugin[hosts.length];
        for (int i = 0; i < hosts.length; ++i) {
            redisPlugin[i] = new RedisPlugin(names[i], hosts[i], Integer.valueOf(ports[i]), passwds[i]);
            redisPlugin[i].start();
        }
    }

    public static void after() throws Exception {
        activeRecord.stop();
        for (int i = 0; i < redisPlugin.length; ++i) {
            redisPlugin[i].stop();
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        before();
        if (aop != null){
            aop.before();
        }
        Object invoke = method.invoke(subject, args);
        if (aop != null){
            aop.after();
        }
        after();
        return invoke;
    }

    public Object getProxy() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?>[] interfaces = subject.getClass().getInterfaces();
        return Proxy.newProxyInstance(loader, interfaces, this);
    }
}
