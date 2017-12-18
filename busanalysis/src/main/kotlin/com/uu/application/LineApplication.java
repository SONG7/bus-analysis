package com.uu.application;

import com.uu.common.proxy.DbBase;
import com.uu.common.proxy.DbSubject;
import com.uu.hadoop.line.LineTracingPointMapper;
import com.uu.hadoop.line.LineTracingPointReduce;
import com.uu.hadoop.line.ShuttleDivisionReducer;
import com.uu.hadoop.line.SourceLocationPointMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

/**
 * Created by song27 on 2017/8/22 0022.
 * <p>
 * MapReduce启动程序
 */
public class LineApplication implements DbSubject {
    private static Job busJob;
    private static Job lineJob;

    /**
     * hadoop运行版本
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        LineApplication lineApplication = new LineApplication();

        DbBase dbBase = new DbBase(lineApplication, new DbBase.Aop() {
            @Override
            public void before() throws IOException {
                System.setProperty("hadoop.home.dir", "D:\\hadoop-2.7.2");

                System.setProperty("HADOOP_USER_NAME", "root");

                Configuration conf = new Configuration();

                busJob = Job.getInstance(conf);
                lineJob = Job.getInstance(conf);

                //设置job所使用的jar包
                conf.set("mapreduce.job.jar", "LineTracingApp.jar");

                //设置busJob中的资源所在的jar包
                busJob.setJarByClass(LineApplication.class);
                lineJob.setJarByClass(LineApplication.class);

                //busJob要使用哪个mapper类
                busJob.setMapperClass(SourceLocationPointMapper.class);
                lineJob.setMapperClass(LineTracingPointMapper.class);
            }

            @Override
            public void after() throws IOException, ClassNotFoundException, InterruptedException {
                String busOutPutPath = "hdfs://hadoop1:9000/line/test1/output/data1";
                String lineOutPutPath = "hdfs://hadoop1:9000/line/test1/output/data2";
                //指定要处理的原始数据所存放的路径hdfs://192.168.0.78:9000/src_data file:\\\C:\Users\Administrator\Desktop\method_request.csv
                FileInputFormat.setInputPaths(busJob, "hdfs://hadoop1:9000/line/test1");
                FileInputFormat.setInputPaths(lineJob, busOutPutPath);

                //指定处理之后的结果输出到哪个路径
                FileOutputFormat.setOutputPath(busJob, new Path(busOutPutPath));
                FileOutputFormat.setOutputPath(lineJob, new Path(lineOutPutPath));

                boolean busRes = busJob.waitForCompletion(true);
                boolean lineRes = false;
                if (busRes){
                    lineRes = lineJob.waitForCompletion(true);
                }

                System.exit(lineRes ? 0 : 1);
            }
        });

        DbSubject dbSubject = (DbSubject) dbBase.getProxy();

        dbSubject.onStart();
    }

    @Override
    public void onStart() {
        //Job要使用哪个reducer类
        busJob.setReducerClass(ShuttleDivisionReducer.class);
        lineJob.setReducerClass(LineTracingPointReduce.class);

        //Job的mapper类输出的kv数据类型
        busJob.setMapOutputKeyClass(Text.class);
        busJob.setMapOutputValueClass(Text.class);

        lineJob.setMapOutputKeyClass(Text.class);
        lineJob.setMapOutputValueClass(Text.class);

        //busJob的reducer类输出的kv数据类型
        busJob.setOutputKeyClass(Text.class);
        busJob.setOutputValueClass(Text.class);

        lineJob.setOutputKeyClass(Text.class);
        lineJob.setOutputValueClass(Text.class);
    }
}
