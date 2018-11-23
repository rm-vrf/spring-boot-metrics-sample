# spring-boot-metrics-sample

Spring Boot 为应用程序收集了一些基本的性能指标，本文将介绍这些指标，并且说明怎样在 Spring Boot 中集成我们自己需要观察的性能指标，以及怎样集中收集这些性能指标，更好的观察系统和集群的性能。

本文使用的 Spring Boot 版本是 1.5.9.RELEASE，涉及到的工具和平台有：OpenTSDB, Elasticsearch, Kibana, Grafana.

## 指标的作用

指标可以监视和测量软件的性能和使用状况，用来诊断错误，跟踪软件的运行过程。有了指标，我们就可以了解软件的性能表现，比如：HTTP 请求的数量/响应时间、任务响应时间、工作流执行时间、内存/CPU 使用状态、数据库连接池的使用状态、某种事件发生的数量、各种级别的日志数量、等等。如果你做过性能测试和性能优化，就很容易理解指标的作用。
距离是
有了指标数据，程序在生产环境上就不再是一个黑盒子。我们可以持续不断的观测软件的性能表现、使用频率、异常数量、其他统计信息，并且可以根据这些指标建立一些预警机制。

一些情况下开发者是没有生产环境的登录权限的，如果软件除了故障，了解故障的原因是一件困难的事情。指标数据可以帮助我们定位故障的原因，找到解决办法。在这方面，日志和跟踪也能起到类似的作用，但是略有不同，指标更多用来解决性能方面的问题。

## 指标的类型

Spring Boot 主要支持两种指标：计数器（Counter）和度量值（Gauge）.

### 计数器

计数器是一个连续增长的数值，可以用计数器来表示：请求的数量、任务完成的数量、异常的数量。

### 度量值

度量值是一个可以即增加也可以减少的数值，可以用度量值来表示：当前时刻的温度、内存使用量。度量值也可以用来表示某些数量，比如执行任务占用的线程数。

## Spring Boot 的指标接口

Spring Boot 的 Actuator Starter 提供了很多生产级的功能特性，包括指标收集和接口。可以在 `/metrics` 端点查看应用程序的指标信息。要启动 Actuator Starter，只要在项目中引入即可：

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

程序启动以后，可以查看性能指标：

```shell
$ curl http://localhost:8080/metrics
{
    "mem": 248771,
    "mem.free": 81008,
    "processors": 4,
    "instance.uptime": 37057,
    "uptime": 43090,
    "systemload.average": 2.26171875,
    "heap.committed": 198656,
    "heap.init": 131072,
    "heap.used": 117647,
    "heap": 1864192,
    "nonheap.committed": 51520,
    "nonheap.init": 2496,
    "nonheap.used": 50115,
    "nonheap": 0,
    "threads.peak": 24,
    "threads.daemon": 19,
    "threads.totalStarted": 27,
    "threads": 22,
    "classes": 6378,
    "classes.loaded": 6378,
    "classes.unloaded": 0,
    "gc.ps_scavenge.count": 7,
    "gc.ps_scavenge.time": 82,
    "gc.ps_marksweep.count": 1,
    "gc.ps_marksweep.time": 40,
    "httpsessions.max": -1,
    "httpsessions.active": 0,
    "gauge.response.metrics": 127,
    "gauge.response.star-star.favicon.ico": 26,
    "counter.status.200.star-star.favicon.ico": 1,
    "counter.status.200.metrics": 1
}
```

Spring Boot 收集了操作系统、内存、Java 垃圾收集、堆内存、类加载器、Tomcat 线程池、会话、HTTP 响应时间和访问次数等各种信息。以 `counter` 开头的是计数器，连续记录事件发生的次数，是增长量；以 `gauge` 开头的是度量值，记录最近一次的数值。

Spring Boot 采用自动配置技术收集更多的指标信息，比如当我们需要在程序中访问数据库，我们会使用 `spring-boot-starter-jdbc`, Spring Boot 会自动收集与数据连接相关的指标，包括：当前活动的连接数、最大连接数、最小连接数等等。所以当我们需要依赖一个组件的时候，应该首先选择 Spring Boot 提供的 Starter.

## 自定义指标

有时候我们需要收集一些关注的指标信息。Spring Boot 提供了 `gaugeService` 和 `counterService`, 可以使用这两个 Bean 报告指标数据。

在代码里引用 Bean：

```java
@Autowired
private GaugeService gaugeService;
	
@Autowired
private CounterService counterService;
```

在需要的时候提交指标数据：

```java
gaugeService.submit("sleep.time", n);

counterService.increment("sleep.count");
```

示例代码直接在运行过程中埋点提交指标数据，这种做法是比较简单的。也可以采用 AOP, Proxy 等一些技术，把埋点这件事做的更高级。

现在把程序运行起来，可以收集到我们自定义的指标：

```shell
$ curl http://localhost:8080/metrics
{
    ...
    "gauge.sleep.time": 1432,
    "counter.sleep.count": 116,
    ...
}
```

## 收集存储

在 `/metrics` 端点上只能查看到一个瞬间的值，并且只能查看到一个程序，当我们需要管理的程序部署在很多节点上，用这种方式是十分不方便的。需要把指标数据集中存储起来，并且使用一些图形化的方式查看，更好的了解程序的性能和使用情况。

Spring Boot Actuator 使用 `MetricExporters` 实现指标数据上传，默认支持的上传方式有：

- Redis
- OpenTSDB
- Statsd
- JMX

Sample 程序把指标上传到 OpenTSDB, 也提供了 Elasticsearch 上传工具。

### OpenTSDB

OpenTSDB 是一个分布式的时间序列数据库，基于 HBase 存储，数据结构设计精致巧妙。[《HBase 实战》](https://www.amazon.cn/dp/B00EHLQAES) 有一章专门讲解了 OpenTSDB 的数据结构。OpenTSDB 非常适合用来存储指标数据。也有人把它用在物联网项目上，存储传感器收集到的数据，电压、流量、温度什么的，使用 OpenTSDB Query API 做数据查询，或者直接用 OpenTSDB 的可视化界面，连界面都不用开发了。

OpenTSDB 默认不为新的指标自动创建 UID，调用 `/api/put` 接口插入一个新的指标会报错。必须预先做一些手工操作，先把指标创建出来。可以修改默认配置实现自动创建，修改配置文件 `/etc/opentsdb/opentsdb.conf`：

```
tsd.core.auto_create_metrics = true
```

考虑到 OpenTSDB 经常用来存储指标数据，指标数据的特点就是类型繁多，这个默认设置实在是非常不合理。

使用 OpenTSDB 收集数据，需要创建一个 `OpenTsdbGaugeWriter` Bean，为他加上 `@ExportMetricWriter` 标签：

```java
@Bean
@ConfigurationProperties("metrics.export")
@ExportMetricWriter
public GaugeWriter writer() {
	return new OpenTsdbGaugeWriter();
}
```

设置 `url` 属性，在配置文件中添加项目：

```shell
metrics.export.url=http://localhost:4242/api/put
```

启动程序之后，每隔 5 秒向 OpenTSDB 上传指标数据。在 OpenTSDB 中查询 `gauge.sleep.time` 指标，看到的数据：

![](https://github.com/lane-cn/spring-boot-metrics-sample/blob/master/images/opentsdb_sleeptime.png?raw=true)

再查看一下 `counter.sleep.count`, 看到的数据是这样的：

![](https://github.com/lane-cn/spring-boot-metrics-sample/blob/master/images/opentsdb_sleepcount_gauge.png?raw=true)

计数器被记录成一个不断增长的量，每次重启程序重置到 0. 这显然不是我们关注的情况。应该记录计数器每次增长的数量。所以我在例子中没有直接使用 `OpenTsdbGaugeWriter`, 而是创建了一个 `OpenTsdbMetricWriter` Bean：

```java
@Bean
@ConfigurationProperties("metrics.export")
@ExportMetricWriter
public MetricWriter metricWriter() {
	return new OpenTsdbMetricWriter();
}
```

`OpenTsdbMetricWriter` 实现了 `MetricWriter` 接口。MetricExporter 对计数器做了特殊处理，可以计算两次报告之间的计数器增长量，把这个增长量报告给 OpenTSDB. 现在 `counter.sleep.count` 的样子正常多了：

![](https://github.com/lane-cn/spring-boot-metrics-sample/blob/master/images/opentsdb_sleepcount.png?raw=true)

> Actuator 的 1.3 之前的版本曾经提供了 `OpenTsdbMetricWriter`，但是在 1.5 以上的版本这个工具消失了，只剩下了一个 `OpenTsdbGaugeWriter`. 2.0 之后的版本已经不再支持 OpenTSDB 了。

> OpenTSDB 要求指标必须有标签，否则存储接口会报错。`DefaultOpenTsdbNamingStrategy` 为指标设置了 `domain` 和 `process` 两个标签。如果实现了新的命名策略，一定要确保为指标设置标签。

### Elasticsearch

使用 Elasticsearch 存储指标数据是一个好办法，可以使用 Elasticsearch 强大的检索和聚合查询能力对程序的性能进行分析。

向 Elasticsearch 上传指标数据，需要创建一个 `ElasticsearchMetricWriter` Bean：

```java
@Bean
@ConfigurationProperties("metrics.export")
@ExportMetricWriter
public MetricWriter metricWriter() {
	return new ElasticsearchMetricWriter();
}
```

设置 `url`, `indexName`, `typeName` 属性，在配置文件中添加项目：

```shell
metrics.export.url=http://localhost:9200
metrics.export.index-name=metrics-{yyyy-MM-dd}
metrics.export.type-name=data
```

索引名称中加上了日期后缀，每天在 Elasticsearch 上创建索引，这也是默认的方式。需要预先估算每天存储的数据量，选择合适的时间段对索引进行分割，对查询性能和数据维护都有好处。

程序启动之后，每 5 秒钟向 Elasticsearch 报告一次指标数据。默认延时可以修改 `spring.metrics.export.delay-millis` 重新设置。如果程序在两次汇报之间被停止，会丢失一部分计数器数据。我们可以降低延时，减少丢失的数据。但是完全避免数据丢失是不可能的。这也是指标数据的设计原则：在满足需求的前提下，为了提高程序本身的运行效率，也为了减少数据量，有少量的数据丢失是可以接受的。

如果需要向其他平台报告指标数据，可以参考 `ElasticsearchMetricWriter` 的代码实现自己的报告工具。可以把指标数据保存到 MySQL、MongoDB，或者文本文件里。

## 数据可视化

OpenTSDB 的图形界面非常简单，功能也不多；Elasticsearch 是没有图形界面的。要查看和分析大量的指标数据，需要一个强大的可视化平台。这里介绍两个平台：Kibana 和 Grafana.

### Kibana

Kibana 是专门针对 Elasticsearch 数据查询分析可视化平台。功能强大，支持折线图、饼图、直方图、热力图、地图。操作简单，是 Elasticsearch 数据可视化分析的默认选择。

![](https://github.com/lane-cn/spring-boot-metrics-sample/blob/master/images/kibana_discover.png?raw=true)

### Grafana

Grafana 是一个时序性数据统计平台，支持的数据源很多。除了刚才提到的 OpenTSDB 和 Elasticsearch，还支持其他数据源：

- Graphite
- InfluxDB
- Microsoft SQL Server
- MySQL
- PostgreSQL
- Prometheus

Grafana 可以把时间序列数据按照折线图、饼图、直方图、热力图等方式展示。并且 Grafana 有强大的插件功能，可以安装插件，扩展更多的数据源和图形功能。

![](https://github.com/lane-cn/spring-boot-metrics-sample/blob/master/images/grafana_edit.png?raw=true)

![](https://github.com/lane-cn/spring-boot-metrics-sample/blob/master/images/grafana_dashboard.png?raw=true)



