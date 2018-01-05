# bus-analysis
公交定位数据分析项目，主要实现根据公交大数据，分析公交车辆在跑那条线路，根据大量定位点，去分析该公交线路的具体轨迹路径


# 启动的步骤
## 1.确保hadoop启动与存取定位数据源
>启动方式参照[hadoop集群实战pdf文档](https://my.oschina.net/song27/blog/1587198)的第3.2章节——启动hadoop。
启动好之后，上传文件到hdfs中去。
```
hadoop fs -put /root/hadoop-data/22lu.csv /line/test1/
```
><b>/root/hadoop-data/22lu.csv</b>是linux文件系统的文件，上传到hdfs的<b>/line/test1/</b>目录下。
执行hdfs的ls指令可查看22lu.csv是否已经在hdfs中。
```
hadoop fs -ls /line/test1/
```
## 2.确保数据库可连接
>运行分析任务的前，需要获取数据库中uu_bus_stations与uu_bus_lines表的数据，也就是需要获得站点数据与线路数据作为参考。<br><b>找到项目下的resources目录下的druid.properties文件，配置mysql连接账号密码跟地址。</b>
## 3.确保redis中保存有站点数据与线路数据
>在运行分析任务的时候多线程并发，并不是每次需要参考数据库mysql的数据，而是访问redis来获得参考数据，所以在运行分析任务之前redis没有站点跟线路数据的时候，执行com.uu.application.FillDataToRedis来把mysql的数据萃取操作，然后存放到redis中。<br>
<b>找到项目下的resources目录下的system.properties文件，配置redis连接密码跟地址。</b>
## 4.启动分析任务
### 4.1 本地java应用执行方式（非hadoop方式）
>该方式仅适合对单个线路或单个车辆的数据进行分析
#### 4.1.1 输入数据文件路径常量的确定
*<b>必须确定该数据文件是同一个线路或同一车辆的定位数据</b>*，在consts.properties中找到一下文件路径常量，修改成自己需要分析的定位数据文件的路径:
``` 
#本地分析任务的输入文件路径
local.analysis.source.input = E:\\workspace\\new\\bus-analysis\\src\\main\\resources\\data\\粤SA2262实时定位点.csv
```
#### 4.1.2 执行本地模式程序
>运行com.uu.application.LocalAnalysisApplication程序,输出方式：
``` json
***************************************
line.id:32
[
    {"lon":113.75046773163753,"lat":23.055426696904547},
    ...
    ,{"lon":113.74868172870724,"lat":23.055455790125823}
]
***************************************
***************************************
line.id:31
[
    {"lon":113.76827154520025,"lat":22.98206217960156},
    ...,
    {"lon":113.76829608556086,"lat":22.982355715887824}
]
***************************************
```
### 4.2 hadoop之上的分析任务
>适合海量定位数据
#### 4.2.1 配置hadoop连接地址，与hdfs输入路径与输出结果的路径
><br>
><b>找到项目下的resources目录下的consts.properties文件，配置相关变量。</b>
```
# hadoop的地址
hadoop.url = hdfs://hadoop2:9000

# 定位数据csv文件所在的根目录
map.reduce.input = /line/test1

# 轨迹分析的班次输出目录，作为轨迹分析二次MapReduce的输入目录
shuttle.reduce.output = /line/test1/output/shuttle

# 轨迹分析最终结果的输出目录
tracing.point.result.output = /line/test1/output/result

# 车辆分类分析最终结果的输出目录
car.line.result.output = /line/test1/output/car
```
>在本文档的第1章节/line/test1/作为原始定位数据，所以hdfs的/line/test1目录下必须有原始数据文件，而后缀是output的文件目录均会在分析之后生成，例如/line/test1/output/shuttle，在/line/test1中并不存在，在分析任务完成后自动生成。

#### 4.2.2 仅仅进行车牌车辆分类的分析任务
>该分析任务对原始数据进行分析，仅仅对车辆是属于哪条线路做出分类，并不做轨迹点线路的路径进行分析。
4.2.1章节中的<b>car.line.result.output</b>常量就是该任务的输出结果的路径，可使用指令：
```
hadoop fs -get /line/test1/output
```
>获取hdfs的文件夹到当前文件夹中<br>
![image](https://note.youdao.com/yws/res/1229/WEBRESOURCE258b7b38f2f21abf7e838fc1aab17afc)<br>
用xftp下载到win下查看，car目录下的part-r-00000就是车辆分类任务的最终输出结果文件。<br>![image](https://note.youdao.com/yws/res/1234/WEBRESOURCE329604d173ec24790e9d32e35b9d67dc)<br>
输出格式是:<br>
粤SA2262	31路<br>
粤SA2283	31路<br>
粤SA2287	31路<br>
粤SA2293	31路<br>
粤SA2298	31路<br>
粤SD5835	31路<br>
粤SD5838	31路<br>
粤SD5850	31路<br>
粤SD5856	31路<br>
粤SD5857	31路<br><br>
>执行com.uu.application.hadoop.CarApplication即可执行车辆分类任务

#### 4.2.3 轨迹分析任务
>consts.properties中的<b>tracing.point.result.output</b>常量即是轨迹分析任务的最终结果输出的目录，输入的原始定位数据目录跟车辆分类任务一样是<b>map.reduce.input</b>。<br>
执行<b>com.uu.application.hadoop.LineApplication</b>，执行完后可以使用指令
```
hadoop fs -get /line/test1/output
```
>获得分析结果，然后使用xftp等工具下载到win本地。同4.2.2一样output下找到输出结果的目录!<br>[image](https://note.youdao.com/yws/res/1260/WEBRESOURCE9f90f3c9b01c08a5622f108f5462f652)<br>
>轨迹分析的结果格式是<br>
lineID:31	[{"lat":22.984803369996936,"lon":113.76309095493292}]<br>
lineID:32	[{"lat":23.058133582759325,"lon":113.74526880963269}]<br>
key是lineID:xxx,value是对应线路ID的轨迹点数组

## 5.数据展示
>把轨迹分析结果的某一个线路ID的轨迹点，由于分析的结果是wgs84标准的定位数据，而网页需要用gcj2的高德坐标系。所以需要进行一次转换才行。
>* 先把轨迹分析结果的value的json数组拷贝到<b>web\data\wgs84-points.json</b>中；
>* 然后执行<b>com.uu.application.GeoMapConvert.kt</b>程序，该程序会读取<b>web\data\wgs84-points.json</b>文件中的数据，转换后写入到<b>web\data\tracing-points.json</b>文件中；
>* 然后执行web根目录下的<b>index.html</b>网页，查看轨迹分析的结果。