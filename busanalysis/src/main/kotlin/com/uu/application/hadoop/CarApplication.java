package com.uu.application.hadoop;

import com.uu.common.constant.Consts;
import com.uu.common.proxy.DbBase;
import com.uu.common.proxy.DbSubject;
import com.uu.hadoop.line.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by song27 on 2017/12/14 0014.
 */
public class CarApplication implements DbSubject {
    private static Job carJob;
    private static Configuration conf = new Configuration();

    public static void main(String[] args) throws Exception {

        CarApplication carApplication = new CarApplication();

        DbBase dbBase = new DbBase(carApplication, new DbBase.Aop() {
            @Override
            public void before() throws IOException {
                System.setProperty("hadoop.home.dir", "D:\\hadoop-2.7.2");

                System.setProperty("HADOOP_USER_NAME", "root");

                carJob = Job.getInstance(conf);

                //设置job所使用的jar包
                conf.set("mapreduce.job.jar", "CarApplication.jar");

                //设置busJob中的资源所在的jar包
                carJob.setJarByClass(CarApplication.class);

                //busJob要使用哪个mapper类
                carJob.setMapperClass(SourceLocationPointMapper.class);
            }

            @Override
            public void after() throws IOException, ClassNotFoundException, InterruptedException, URISyntaxException {
                String carOutPutPath = String.format("%s%s", Consts.HADOOP_URL, Consts.CAR_LINE_RESULT_OUTPUT_PATH);
                FileSystem fileSystem = FileSystem.get(new URI(Consts.HADOOP_URL), conf);
                fileSystem.delete(new Path(Consts.CAR_LINE_RESULT_OUTPUT_PATH), true);

                //指定要处理的原始数据所存放的路径
                FileInputFormat.setInputPaths(carJob, String.format("%s%s", Consts.HADOOP_URL, Consts.MAP_REDUCE_INPUT_PATH));

                //指定处理之后的结果输出到哪个路径
                FileOutputFormat.setOutputPath(carJob, new Path(carOutPutPath));

                boolean carRes = carJob.waitForCompletion(true);
                System.exit(carRes ? 0 : 1);
            }
        });

        DbSubject dbSubject = (DbSubject) dbBase.getProxy();

        dbSubject.onStart();
    }

    @Override
    public void onStart() {
        //Job要使用哪个reducer类
        carJob.setReducerClass(LineAnalyzerReduce.class);

        //Job的mapper类输出的kv数据类型
        carJob.setMapOutputKeyClass(Text.class);
        carJob.setMapOutputValueClass(Text.class);

        //busJob的reducer类输出的kv数据类型
        carJob.setOutputKeyClass(Text.class);
        carJob.setOutputValueClass(Text.class);
    }
}
