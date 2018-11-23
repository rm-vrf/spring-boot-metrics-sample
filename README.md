# spring-boot-metrics-sample

Spring Boot 为应用程序收集基本性能指标。本文将介绍这些指标，并且说明怎样集成自定义的指标，以及怎样集中收集存储性能指标。

例子使用的 Spring Boot 版本是 1.5.9.RELEASE.

## 指标的作用

指标可以用来测量软件的性能和使用状况。有了指标，我们就可以了解软件的执行细节，比如：HTTP 请求的次数和响应时间、任务执行时间、内存和CPU 的使用状态、数据库连接池的使用状态、某个级别的日志数量、等等。如果你做过性能测试和性能优化，就很容易理解指标的作用。

有了指标，程序在生产环境上就不再是一个黑盒子。我们可以持续不断的观测软件的性能表现、使用频率、异常数量，并且可以根据这些数值建立一些预警机制。

一些情况下开发者是没有生产环境的登录权限的，如果软件出了故障，了解故障的原因是一件非常困难的事情。指标可以帮助我们定位故障原因，找到解决办法。日志和跟踪也能起到类似的作用，指标更多用来解决性能故障。

## 指标的类型

Spring Boot 主要支持两种类型的指标：计数器（Counter）和度量值（Gauge）.

### 计数器

计数器是一个连续增长的数值，可以用来表示：请求的数量、任务完成的数量、异常的数量。

### 度量值

度量值是一个即可以增加也可以减少的数值，可以用度量值来表示：当前时刻的温度、内存使用量。

## Spring Boot 的指标接口

Spring Boot 的 Actuator Starter 提供了很多生产级功能，包括指标接口。可以在 `/metrics` 端点查看指标。要启动 Actuator Starter，只要在项目中引入依赖库即可：

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

程序启动之后，可以查看性能指标：

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

Spring Boot 收集操作系统、内存、Java 垃圾收集、堆内存、类加载器、Tomcat 线程池、会话、HTTP 响应时间和访问次数等指标。以 `counter` 开头的是计数器，记录从程序启动到当前时间的总数；以 `gauge` 开头的是度量值，记录最近一次的数值。

Spring Boot 采用自动配置技术收集指标。比如我们在程序中访问数据库，会使用 `spring-boot-starter-jdbc`. Spring Boot 会自动收集相关的指标，包括：当前活动的连接数、最大连接数、最小连接数等等。当我们使用一个组件的时候，应该首先选择 Spring Boot 提供的 Starter.

## 自定义指标

有时候我们需要收集自定义指标。Spring Boot 提供了 `gaugeService` 和 `counterService`, 可以使用他们收集指标。

在代码里引用 Bean：

```java
@Autowired
private GaugeService gaugeService;
	
@Autowired
private CounterService counterService;
```

收集指标：

```java
gaugeService.submit("sleep.time", n);

counterService.increment("sleep.count");
```

示例代码埋点提交指标数据，这种做法是比较直接的。也可以采用 AOP, Proxy 等一些技术，把这件事做的更高级。现在把程序运行起来，可以收集到我们自定义的指标：

```shell
$ curl http://localhost:8080/metrics
{
    ...
    "gauge.sleep.time": 1432,
    "counter.sleep.count": 116,
    ...
}
```

## 集中存储

在 `/metrics` 端点上只能查看到一个瞬间的值，并且一次只能查看一个程序。当程序部署在很多节点上，用这种方式是十分不方便的。需要把指标数据集中存储起来，使用一些图形化的查看工具，更好的分析程序的性能。

Spring Boot Actuator 使用 MetricExporter 实现指标数据上传，默认支持的存储平台有：

- Redis
- OpenTSDB
- Statsd
- JMX

示例代码把指标上传到 OpenTSDB, 也提供了 Elasticsearch 上传代码。

### OpenTSDB

OpenTSDB 是一个分布式时间序列数据库，他基于 HBase 分布式存储，数据结构设计精致巧妙，具有极高的吞吐量和查询速度。[《HBase 实战》](https://www.amazon.cn/dp/B00EHLQAES) 有一章专门讲解了 OpenTSDB 的设计。围绕 OpenTSDB 开发了一些监控工具，包括数据收集、图形化分析。也有人把它用在物联网产品，存储传感器收集到的数据，电压、流量、温度数据。使用 OpenTSDB Query API 做数据查询，或者直接用 OpenTSDB 的可视化界面，连界面都不用开发了。

> OpenTSDB 默认不为新的指标自动创建 UID，调用 `/api/put` 接口插入一个新指标会报错。必须预先创建指标。
> 可以修改默认配置，实现自动创建。修改配置文件 `/etc/opentsdb/opentsdb.conf`：
> 
> tsd.core.auto_create_metrics = true
> 
> 考虑到 OpenTSDB 经常用来存储指标，指标数据的特点就是类型繁多，这个默认设置实在是非常不合理。

使用 OpenTSDB 收集数据，需要创建一个 `OpenTsdbGaugeWriter` Bean，为他加上 `@ExportMetricWriter` 标签：

```java
@Bean
@ConfigurationProperties("metrics.export")
@ExportMetricWriter
public GaugeWriter writer() {
	return new OpenTsdbGaugeWriter();
}
```

设置 `url` 属性，在配置文件中设置：

```shell
metrics.export.url=http://localhost:4242/api/put
```

启动程序之后，每隔 5 秒向 OpenTSDB 上传一次数据。在 OpenTSDB 中查询 `gauge.sleep.time` 指标，看到的数据：

![](https://github.com/lane-cn/spring-boot-metrics-sample/blob/master/images/opentsdb_sleeptime.png?raw=true)

效果很好。再查看一下 `counter.sleep.count`, 看到的数据是这样的：

![](https://github.com/lane-cn/spring-boot-metrics-sample/blob/master/images/opentsdb_sleepcount_gauge.png?raw=true)

这不是我们希望的样子。计数器被记录成一个不断增长的量，每次重启程序重置为 0. 为了记录计数器每次增长的数量，示例程序没有直接使用 `OpenTsdbGaugeWriter`, 而是创建了一个 `OpenTsdbMetricWriter` Bean：

```java
@Bean
@ConfigurationProperties("metrics.export")
@ExportMetricWriter
public MetricWriter metricWriter() {
	return new OpenTsdbMetricWriter();
}
```

`OpenTsdbMetricWriter` 实现了 `MetricWriter` 接口。MetricExporter 对计数器做了特殊处理，自动计算两次报告之间的增长量。现在 `counter.sleep.count` 的数据正常了：

![](https://github.com/lane-cn/spring-boot-metrics-sample/blob/master/images/opentsdb_sleepcount.png?raw=true)

> Actuator 1.3 版本提供了 `OpenTsdbMetricWriter`，但是在 1.5 版本这个工具消失了，只剩下了一个 `OpenTsdbGaugeWriter`. 2.0 之后的版本已经不再内置支持 OpenTSDB.

> OpenTSDB 要求指标必须有标签，否则存储接口会报错。`DefaultOpenTsdbNamingStrategy` 为指标设置了 `domain` 和 `process` 两个标签。如果选择其他命名策略，要确保为指标设置标签。

### Elasticsearch

使用 Elasticsearch 存储指标，可以利用 Elasticsearch 强大的检索和聚合查询能力。向 Elasticsearch 上传指标数据，需要创建一个 `ElasticsearchMetricWriter` Bean：

```java
@Bean
@ConfigurationProperties("metrics.export")
@ExportMetricWriter
public MetricWriter metricWriter() {
	return new ElasticsearchMetricWriter();
}
```

设置 `url`, `indexName`, `typeName` 属性，在配置文件中设置：

```shell
metrics.export.url=http://localhost:9200
metrics.export.index-name=metrics-{yyyy-MM-dd}
metrics.export.type-name=data
```

> 索引名称加上了日期后缀，每天在 Elasticsearch 上创建一个索引，这是默认的方式。
> 开发者需要预先估算数据量，选择合适的时间区间对索引进行分割。合理的分割对性能和数据维护都有好处。

> 程序启动之后，每 5 秒钟向 Elasticsearch 报告一次指标数据。延时可以修改 `spring.metrics.export.delay-millis` 重新设置。
> 如果程序在两次汇报之间停止，会丢失最近一次计数器数据。我们可以降低延时，减少丢失的数据。但是完全避免数据丢失是不可能的。
> 在满足基本需求的前提下，为了提高程序本身的运行效率，也为了降低存储平台的压力，丢失少量数据是可以接受的。

如果使用其他存储平台，可以参考 `ElasticsearchMetricWriter`. 可以把指标数据保存到 MySQL、MongoDB，或者文本文件里。

## 数据可视化

OpenTSDB 的图形界面非常简单，功能也不多；Elasticsearch 是没有图形界面的。要查看和分析大量的指标数据，需要一个强大的可视化平台。这里介绍两个：Kibana 和 Grafana.

### Kibana

Kibana 是专门针对 Elasticsearch 的数据查询分析平台。Kibana 功能强大，支持折线图、饼图、直方图、热力图、地图，并且操作简单，是 Elasticsearch 数据可视化的默认选择。

![](https://github.com/lane-cn/spring-boot-metrics-sample/blob/master/images/kibana_discover.png?raw=true)

### Grafana

Grafana 是一个时序数据统计平台，支持的数据源非常多。除了刚才提到的 OpenTSDB 和 Elasticsearch，还支持其他数据源，比如：

- Graphite
- InfluxDB
- Microsoft SQL Server
- MySQL
- PostgreSQL
- Prometheus

Grafana 可以把时间序列数据按照折线图、饼图、直方图、热力图等方式展示。并且 Grafana 有强大的插件功能，可以扩展更多的数据源和图形功能。

![](https://github.com/lane-cn/spring-boot-metrics-sample/blob/master/images/grafana_edit.png?raw=true)

![](https://github.com/lane-cn/spring-boot-metrics-sample/blob/master/images/grafana_dashboard.png?raw=true)
