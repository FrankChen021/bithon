namespace java com.sbss.bithon.agent.rpc.thrift.service.metric.message


/***************************************************************************/
/*************************** Web Request Metrics ***************************/
/***************************************************************************/
struct WebRequestMessage {
    1:string appName;
    2:string env;
    3:string hostName;
    4:i32 port;
    5:i32 interval;
    6:i64 timestamp;
    7:WebRequestEntity requestEntity;
}

struct WebRequestEntity {
    1:optional string srcApplication;
    2:string uri;
    3:i64 costTime;
    4:i64 requestCount;
    5:i64 errorCount;
    6:i64 count4xx;
    7:i64 count5xx;
    8:i64 requestByteSize;
    9:i64 responseByteSize;
}

/***************************************************************************/
/*************************** JVM Metrics ***********************************/
/***************************************************************************/
struct JvmMessage {
    1:string appName;
    2:string env;
    3:string hostName;
    4:i32 port;
    5:i32 interval;
    6:i64 timestamp;
    7:InstanceTimeEntity instanceTimeEntity;
    8:CpuEntity cpuEntity;
    9:MemoryEntity memoryEntity;
    10:HeapEntity heapEntity;
    11:NonHeapEntity nonHeapEntity;
    12:list<GcEntity> gcEntities;
    13:ThreadEntity threadEntity;
    14:ClassEntity classesEntity;
    15:MetaspaceEntity metaspaceEntity;
}

struct InstanceTimeEntity {
    // 应用实例运行时间，即不包含启动时间（单位：毫秒）
    1:i64 instanceUpTime;
    // 系统正常运行时间，即包含启动时间（单位：毫秒）
    2:i64 instanceStartTime;
}

struct CpuEntity {
    // CPU内核数
    1:i64 processors;
    // CPU处理时间（单位：纳秒）
    2:i64 processCpuTime;
    // 处理器的负载均值
    3:double systemloadAverage;
    // cpu使用情况（单位：百分比）
    4:double processCpuLoad;
}

struct MemoryEntity {
    // 分配给应用的总内存数（单位：字节）
    1:i64 mem;
    // 当前空闲的内存数（单位：字节）
    2:i64 free;
}

struct HeapEntity {
    // 约等于-Xmx的值（单位：字节）
    1:i64 heap;
    // 约等于-Xms的值（单位：字节）
    2:i64 heapInit;
    // 已经被使用的内存大小（单位：字节）
    3:i64 heapUsed;
    // 当前可使用的内存大小，包括used（单位：字节）
    4:i64 heapCommitted;
}

struct NonHeapEntity {
    // 约等于XX:MaxPermSize的值（单位：字节）
    1:i64 nonHeap;
    // 约等于-XX:PermSize的值（单位：字节）
    2:i64 nonHeapInit;
    // 已经被使用的内存大小（单位：字节）
    3:i64 nonHeapUsed;
    // 当前可使用的内存大小，包括used（单位：字节）
    4:i64 nonHeapCommitted;
}

struct GcEntity {
    // 垃圾回收名称
    1:optional string gcName;
    // 垃圾回收次数
    2:i64 gcCount;
    // 垃圾回收耗时
    3:i64 gcTime;
    // GC分类0-新生代、1-老年代
    4:optional i32 generation;
}

struct ThreadEntity {
    // 从JVM启动或峰值重置以来峰值活动线程计数
    1:i64 peakThreads;
    // 活动守护线程的当前数目
    2:i64 daemonThreads;
    // 从JVM启动以来创建和启动的线程总数目
    3:i64 totalThreads;
    // 活动线程的当前数目，包括守护线程和非守护线程
    4:i64 activeThreads;
}

struct ClassEntity {
    // 当前加载到JVM中的类的数量
    1:i64 classes;
    // 自JVM开始执行到目前已经加载的类的总数
    2:i64 loaded;
    // 自JVM开始执行到目前已经卸载的类的总数
    3:i64 unloaded;
}

struct MetaspaceEntity {
    // metaspace已分配的内存大小（单位：字节）
    1:i64 metaspaceCommitted;
    // metaspace已使用的内存大小（单位：字节）
    2:i64 metaspaceUsed;
}


/***************************************************************************/
/************************* Web Server Metrics ******************************/
/***************************************************************************/

struct WebServerMessage {
    1:string appName;
    2:string env;
    3:string hostName;
    4:i32 port;
    5:i32 interval;
    6:i64 timestamp;
    7:WebServerEntity serverEntity;
}

struct WebServerEntity {
    1:i64 connectionCount;
    2:i64 maxConnections;
    3:i64 activeThreads;
    4:i64 maxThreads;
    5:string type;
}

/***************************************************************************/
/***************************  Application Exception ************************/
/**************************************************************************/

struct ExceptionMessage {
    1:string appName;
    2:string env;
    3:string hostName;
    4:i32 port;
    5:i32 interval;
    6:i64 timestamp;
    7:list<ExceptionEntity> exceptionList;
}

struct ExceptionEntity {
    1:string className;
    2:string message;
    3:string stackTrace;
    4:string uri;
    5:i64 exceptionCount;
}

/***************************************************************************/
/******************* Http Client *******************************************/
/**************************************************************************/
struct HttpClientMessage {
    1:string appName;
    2:string env;
    3:string hostName;
    4:i32 port;
    5:i32 interval;
    6:i64 timestamp;
    7:HttpClientEntity httpClient;
}

struct HttpClientEntity {
    1:string uri;
    2:string method;
    3:i64 costTime;
    4:i64 count4xx;
    5:i64 count5xx;
    6:i64 requestCount;
    7:i64 exceptionCount;
    8:i64 requestBytes;
    9:i64 responseBytes;
}

/***************************************************************************/
/******************* Thread Pool *******************************************/
/**************************************************************************/
struct ThreadPoolMessage {
    1:string appName;
    2:string env;
    3:string hostName;
    4:i32 port;
    5:i32 interval;
    6:i64 timestamp;
    7:list<ThreadPoolEntity> pools;
}

struct ThreadPoolEntity {
    1:string executorClass;
    2:string poolName;
    3:i64 callerRunTaskCount;
    4:i64 abortedTaskCount;
    5:i64 discardedTaskCount;
    6:i64 discardedOldestTaskCount;
    7:i64 exceptionTaskCount;
    8:i64 successfulTaskCount;
    9:i64 totalTaskCount;
    10:i64 activeThreads;
    11:i64 currentPoolSize;
    12:i64 maxPoolSize;
    13:i64 largestPoolSize;
    14:i64 queuedTaskCount;
}

///***************************************************************************/
///*******************************  JDBC Connection***************************/
///**************************************************************************/
//
struct JdbcMessage {
    1:string appName;
    2:string env;
    3:string hostName;
    4:i32 port;
    5:i32 interval;
    6:i64 timestamp;
    7:list<JdbcEntity> jdbcList;
}

struct JdbcEntity {
    7:string uri;
    8:string driver;
    9:i64 activeCount;
    11:optional i64 createCount;
    12:optional i64 destroyCount;
    13:optional i64 poolingCount;
    14:optional i64 poolingPeak;
    15:optional i64 activePeak;
    16:optional i64 logicConnectCount;
    17:optional i64 logicCloseCount;
    18:optional i64 waitThreadCount;
    19:optional i64 createErrorCount;
    20:optional i64 executeCount;
    21:optional i64 commitCount;
    22:optional i64 rollbackCount;
    23:optional i64 startTransactionCount;
}

struct RedisMessage {
    1:string appName;
    2:string env;
    3:string hostName;
    4:i32 port;
    5:i32 interval;
    6:i64 timestamp;
    7:list<RedisEntity> redisList;
}

struct RedisEntity {
    7:string uri;
    8:string command;
    9:i64 exceptionCount;
    10:i64 totalCount;
    11:i64 requestTime;
    12:i64 responseTime;
    13:i64 requestBytes;
    14:i64 responseBytes;
}

//    // 应用名称
//    1:string appName;
//    // 主机名称
//    2:string hostName;
//    // 端口
//    3:i32 port;
//    // 时间戳
//    4:i64 timestamp;
//    // 采集数据间隔（单位：秒）
//    5:i32 interval;
//    // 分类标识
//    6:string category;
//    // sql性能
//    7:SqlPerformanceEntity sqlPerformanceEntity;
//    // 请求byte size
//    8:optional i64 requestByteSize;
//    // 响应byte size
//    9:optional i64 responseByteSize;
//    // 聚合粒度：
//    10:optional i32 GranularityType;
//    // driverType 分类
//    11:optional string driverType;
//}
//
//struct SqlPerformanceEntity {
//    // SQL执行时间（单位：纳秒）
//    1:i64 costTime;
//    // 失败数量
//    2:i32 failureCount;
//    // sql统计数
//    3:i32 total;
//    // 查询次数
//    4:i32 queryCount;
//    // 写入次数
//    5:i32 updateCount;
//    // sql主机IP和地址
//    6:string hostPort;
//}
//
//
///***************************************************************************/
///******************************  Quartz连接  *******************************/
///**************************************************************************/
//
//struct QuartzEntity {
//    // 应用名称
//    1:string appName;
//    // 主机名称
//    2:string hostName;
//    // 端口
//    3:i32 port;
//    // 时间戳
//    4:i64 timestamp;
//    // 采集时间间隔（单位：秒）
//    5:i32 interval;
//    // 分类标识
//    6:string category;
//    // 采集次数
//    7:i32 collectCount;
//    // 任务总数
//    8:i32 jobsTotal;
//    // 已执行任务数
//    9:i32 jobsExecuted;
//    // 正在执行任务数
//    10:i32 jobsCurrentlyExecuting;
//    // 暂停任务数
//    11:i32 jobsPause;
//    // 阻塞任务数
//    12:i32 jobsBlock;
//    // 失败任务数
//    13:i32 jobsFail;
//    // 聚合粒度：
//    14:optional i32 GranularityType;
//}
//
///***************************************************************************/
///******************************  中间件Database信息  ************************/
///**************************************************************************/
//
//struct MiddlewareEntity {
//    // 分类标识
//    1:string category;
//    // Redis
//    2:RedisEntity redisEntity;
//    // MongoDB
//    3:MongoDBEntity mongoDBEntity;
//    // Mysql
//    4:MysqlEntity mysqlEntity;
//    // 聚合粒度：
//    5:optional i32 GranularityType;
//}
//

//
//struct MongoDBEntity {
//    // 连接数据库应用名称
//    1:string appName;
//    // 应用主机IP
//    2:string hostName;
//    // 应用端口号
//    3:i32 port;
//    // 数据库IP和端口号
//    4:string hostPort;
//    // 时间戳
//    5:i64 timestamp;
//    // 采集时间间隔（单位：秒）
//    6:i32 interval;
//    // 执行命令数
//    7:i32 commands;
//    // 命令错误数
//    8:i32 failureCount;
//    // 请求耗时（单位：纳秒）
//    9:i64 costTime;
//    // MongoDB数据库标识
//    10:optional string flag = "1";
//    // 请求byte size
//    11:i64 requestByteSize;
//    // 响应byte size
//    12:i64 responseByteSize;
//}
//
//struct MysqlEntity {
//    // 连接数据库应用名称
//    1:string appName;
//    // 应用主机IP
//    2:string hostName;
//    // 应用端口号
//    3:i32 port;
//    // 数据库IP和端口号
//    4:string hostPort;
//    // 时间戳
//    5:i64 timestamp;
//    // 采集时间间隔（单位：秒）
//    6:i32 interval;
//    // 执行命令数
//    7:i32 commands;
//    // 命令错误数
//    8:i32 failureCount;
//    // 请求耗时（单位：纳秒）
//    9:i64 costTime;
//    // Mysql数据库标识
//    10:optional string flag = "2";
//}
//
//
///***************************************************************************/
///******************************  集群服务Database信息  **********************/
///**************************************************************************/
//
//struct ClusterEntity {
//    // 分类标识
//    1:string category;
//    // Redis集群服务
//    2:RedisClusterEntity redisClusterEntity;
//    // MongoDB集群服务
//    3:MongoDBClusterEntity mongoDBClusterEntity;
//    // Mysql集群服务
//    4:MysqlClusterEntity mysqlClusterEntity;
//    // 聚合粒度：
//    5:optional i32 GranularityType;
//}
//
//struct RedisClusterEntity {
//    // 集群名称
//    1:string clusterName;
//    // 时间戳
//    2:i64 timestamp;
//    // 采集时间间隔（单位：秒）
//    3:i32 interval;
//    // 连接数
//    4:i32 connections;
//    // 从库连接数
//    5:i32 slaveConnections;
//    // 阻塞连接数
//    6:i32 blockConnections;
//    // Pub/Sub通道数
//    7:i32 pubSub;
//    // Pub/Sub模式数
//    8:i32 pubSubMode;
//    // 总命中次数
//    9:i64 hits;
//    // 未命中次数
//    10:i64 misses;
//    // 使用内存（单位：字节）
//    11:i64 mem;
//    // 执行命令数
//    12:i64 totalCommands;
//    // 输入量（单位：字节）
//    13:i64 totalNetworkBytesIn;
//    // 输出量（单位：字节）
//    14:i64 totalNetworkBytesOut;
//    // Redis集群数据库标识
//    15:optional string flag = "0";
//    // Redis节点地址
//    16:optional string host;
//    // Redis节点端口
//    17:optional i32 port;
//}
//
//struct MongoDBClusterEntity {
//    // 副本集名称
//    1:string replicaSetName;
//    // 所有节点列表（主机名：端口号）
//    2:list<string> replicaHosts;
//    // 时间戳
//    3:i64 timestamp;
//    // 采集时间间隔（单位：秒）
//    4:i32 interval;
//    // 当前连接数
//    5:i32 currentConnections;
//    // 可用连接数
//    6:i32 availableConnections;
//    // 当前使用的内存（单位：字节）
//    7:i64 residentMem;
//    // 当前等待读锁数
//    8:i32 readLocks;
//    // 当前等待写锁数
//    9:i32 writeLocks;
//    // 索引被访问并从内存中返回的次数
//    10:i32 indexHits;
//    // 索引被访问但是不在内存中的次数
//    11:i32 indexMisses;
//    // 输入量（单位：字节）
//    12:i64 totalNetworkBytesIn;
//    // 输出量（单位：字节）
//    13:i64 totalNetworkBytesOut
//    // 从启动后插入操作的总数
//    14:i64 insertCounter;
//    // 从启动后查询操作的总数
//    15:i64 queryCounter;
//    // 从启动后更新操作的总数
//    16:i64 updateCounter;
//    // 从启动后删除操作的总数
//    17:i64 deleteCounter;
//    // 访问数据时发现数据不在内存时的页面数量
//    18:i64 pageFaults;
//    // MongoDB集群数据库标识
//    19:optional string flag = "1";
//    // MongoDB节点地址
//    20:optional string host;
//    // MongoDB节点端口
//    21:optional i32 port;
//}
//
//struct MysqlClusterEntity {
//    // 数据库主机IP
//    1:string hostName;
//    // 数据库端口号
//    2:i32 port;
//    // 时间戳
//    3:i64 timestamp;
//    // 采集时间间隔（单位：秒）
//    4:i32 interval;
//    // select吞吐率
//    5:i32 selectThroughPut;
//    // insert吞吐率
//    6:i32 insertThroughPut;
//    // update吞吐率
//    7:i32 updateThroughPut;
//    // delete吞吐率
//    8:i32 deleteThroughPut;
//    // 持久化连接利用率
//    9:double connUsedPercent;
//    // 查询缓存空间使用率
//    10:double cacheUsedPercent;
//    // 索引缓存命中率
//    11:double cacheHits;
//    // 缓存查询数
//    12:i32 cacheQueries;
//    // 索引缓存命中率
//    13:double indexCacheHits;
//    // 索引读取统计
//    14:i32 indexReadStatis;
//    // 连接吞吐率，即每秒钟连接数
//    15:i32 connThroughPut;
//    // 连接缓存命中率
//    16:i32 connCacheHits;
//    // 并发连接数，包括最大允许连接数、实际最大连接数、当前连接数、活跃连接数、缓存连接数
//    17:i32 concurrentConn;
//    // 流量统计
//    18:i64 trafficStatis;
//    // 表统计锁定
//    19:i32 lockStatis;
//    // Mysql集群数据库标识
//    20:optional string flag = "2";
//}
//
///***************************************************************************/
///************************** 应用调用Kafka **********************************/
///**************************************************************************/
//
//struct AppKafkaEntity {
//    // 连接kafka的应用名称
//    1:string appName;
//    // 应用主机IP
//    2:string hostName;
//    // 应用端口号
//    3:i32 port;
//    // 时间戳
//    4:i64 timestamp;
//    // 采集时间间隔
//    5:i32 interval;
//    // 分类标识
//    6:string category;
//    // Kafka消费者
//    7:list<KafkaConsumerEntity> kafkaConsumerList;
//    // Kafka生产者
//    8:list<KafkaProducerEntity> kafkaProducerList;
//    // Kafka网络吞吐量
//    9:list<KafkaNetworkEntity> kafkaNetworkList;
//    // 聚合粒度：
//    10:optional i32 GranularityType;
//}
//
//struct KafkaConsumerEntity {
//    // 分组
//    1:string groupId;
//    // 主题
//    2:string topic;
//    // 区块
//    3:i32 partition;
//    // 消费数
//    4:i32 consumeCount;
//}
//
//struct KafkaProducerEntity {
//    // 主题
//    1:string topic;
//    // 区块
//    2:i32 partition;
//    // 请求次数
//    3:i32 sendCount;
//}
//
//struct KafkaNetworkEntity {
//    // 请求次数
//    1:i32 requestCount;
//    // 响应次数
//    2:i32 responseCount;
//    // 请求总耗时
//    3:i64 costTime;
//    // 发送字节数
//    4:i64 bytesSent;
//    // 接收字节数
//    5:i64 bytesReceive;
//    // 请求次数
//    6:i32 pollCount;
//}
//
///***************************************************************************/
///************************** Kafka 集群 *************************************/
///**************************************************************************/
//
//struct KafkaEntity {
//    // 分类标识
//    1:string category;
//    // Kafka Topic Consumer
//    2:optional list<KafkaTopicConsumerEntity> topicConsumerEntity;
//    // kafka集群
//    3:optional list<KafkaClusterEntity> kafkaClusterEntity;
//    // 聚合粒度：
//    4:optional i32 GranularityType;
//}
//
//struct KafkaClusterEntity {
//    // 集群名称
//    1:string clusterId;
//    // brokerId 或 topicName
//    2:string nameId;
//    // 主机IP地址
//    3:string hostName;
//    // 主机端口
//    4:i32 port;
//    // 采集时间戳
//    5:i64 timestamp;
//    // 采集时间间隔（单位：秒）
//    6:i32 interval;
//    // 每秒写入条数
//    7:double messageIn;
//    // 每秒写入字节数（单位：字节）
//    8:i64 bytesIn;
//    // 每秒写出字节数（单位：字节）
//    9:i64 bytesOut;
//    // 每秒拒绝字节数（单位：字节）
//    10:i64 bytesRejected;
//    // 每秒生产者请求次数
//    11:i32 produceRequests;
//    // 每秒消费者拉取次数
//    12:i32 fetchRequests;
//    // 类型：1-broker、2-topic，默认为topic
//    13:string type = "2";
//}
//
//struct KafkaTopicConsumerEntity {
//    // 集群ID
//    1:string clusterId;
//    // topic名称
//    2:string nameId;
//    // 分组ID
//    3:string groupId;
//    // 分区
//    4:i32 partition;
//    // 主机地址
//    5:string hostName;
//    // 主机端口
//    6:i32 port;
//    // 采集时间戳
//    7:i64 timestamp;
//    // 采集时间间隔(单位：秒）
//    8:i32 interval;
//    // 消息堆积数
//    9:i64 lag;
//    // 类型：1-broker、2-topic，默认为topic
//    10:string type = "2";
//}
//

//
///***************************************************************************/
///*****************************  Zookeeepr指标  *****************************/
///**************************************************************************/
//
//struct ZookeeperEntity {
//    // 服务端主机地址端口
//    1:string name;
//    // 采集时间间隔
//    2:i32 interval;
//    // 时间戳
//    3:i64 timestamp;
//    // 分类标识
//    4:string category;
//    // 活跃连接数
//    5:i32 numAliveConnections;
//    // 发包数
//    6:i64 packetsSent;
//    // 收包数
//    7:i64 packetsReceived;
//    // 请求堆积数
//    8:i64 outstandingRequests;
//    // 最大请求延时
//    9:i64 maxRequestLatency;
//    // 最小请求延时
//    10:i64 minRequestLatency;
//    // 平均请求延时
//    11:i64 avgRequestLatency;
//    // 聚合粒度：
//    12:optional i32 GranularityType;
//    // Zookeeper节点地址
//    13:optional string host;
//    // Zookeeper节点端口
//    14:optional i32 port;
//}
//
///***************************************************************************/
///******************************  扩展指标信息  ******************************/
///**************************************************************************/
//
//struct ExtendEntity {
//    // 应用名称
//    1:string appName
//    // 主机名称
//    2:string hostName;
//    // 端口
//    3:i32 port;
//    // 时间戳
//    4:i64 timestamp;
//    // 采集时间间隔（单位：秒）
//    5:i32 interval;
//    // 采集次数
//    6:i32 collectCount;
//    7:string entityName;
//    8:map<string, string> stringFields;
//    9:map<string, i64> longFields;
//}
//
///***************************************************************************/
///*************************** Quartz Job&Trigger列表和执行日志  **************/
///**************************************************************************/
//
//struct QuartzInfoEntity {
//    // 应用名称
//    1:optional string appName
//    // 主机名称
//    2:optional string hostName;
//    // 端口
//    3:optional i32 port;
//    // Job&Trigger列表
//    4:list<JobTriggerEntity> jobTriggerList;
//    // Job&Trigger执行日志列表
//    5:list<map<string, string>> jobTriggerLogList;
//}
//
//struct JobTriggerEntity {
//    // 任务名称
//    1:string jobName;
//    // 任务Key
//    2:string jobKey;
//    // 任务描述
//    3:string jobDescription;
//    // 任务分组
//    4:string jobGroup;
//    // 任务类
//    5:string jobClass;
//    // 执行时间表达式
//    6:string triggerCron;
//    // 执行状态
//    7:string triggerStatus;
//    // 上次执行时间
//    8:i64 prevFireTime;
//    // 下次执行时间
//    9:i64 nextFireTime;
//    // 是否周期性任务
//    10:bool isDurable;
//    // 是否执行后持久化JobData
//    11:bool isPersistJobDataAfterExecution;
//    // 是否要禁止并发执行
//    12:bool isConcurrentExecutionDisallowed
//    // 是否需要重试
//    13:bool requestsRecovery;
//}
//
///***************************************************************************/
///***************************  JVM明细和系统明细  ****************************/
///**************************************************************************/
//
//struct DetailEntity {
//    // JVM明细
//    1:map<string, string> jvmDetail;
//    // 系统明细
//    2:map<string, string> systemDetail;
//}
//
//


//
//
///***************************************************************************/
///******************* ZBRD Http  *****************/
///**************************************************************************/
//
//struct ZbrdHttpEntity {
//    // zbrd 机器ip
//    1:string host;
//    // 时间戳
//    2:i64 timestamp;
//    // 请求域名
//    3:string targetDomain;
//    // 请求地址
//    4:string targetUri;
//    // 请求耗时
//    5:i64 costTime;
//    // 4xx请求类型失败数
//    6:i32 count4xx;
//    // 5xx请求类型失败数
//    7:i32 count5xx;
//    // 请求次数
//    8:i32 requestCount;
//    // 请求byte size
//    9:i64 requestByteSize;
//    // 响应byte size
//    10:i64 responseByteSize;
//    // 请求方式(0-get、1-post、2-put、3-patch、4-delete、5-options、6-head）
//    11:string method;
//    // 请求来源，即请求发起端应用名称
//    12:string appName;
//    // 分类标识
//    13:string category;
//    // 聚合粒度：
//    14:optional i32 GranularityType;
//}
//
///***************************************************************************/
///********************  Spring Restful Uri Pattern收集  *********************/
///***************************************************************************/
//
//struct SpringRestfulUriPatternEntity {
//    // 应用名称
//    1:string appName;
//    // 主机地址
//    2:string hostName;
//    // 端口号
//    3:i32 port;
//    // uri pattern时间戳（单位：毫秒）
//    4:i64 timestamp;
//    // restful uri pattern list
//    5:list<string> uriPatternList;
//}
//
///***************************************************************************/
///****************************  Channel Metrics收集  ************************/
///***************************************************************************/
//
//struct ChannelEntity {
//    // 应用名称
//    1:string appName;
//    // 主机名称
//    2:string hostName;
//    // 端口
//    3:i32 port;
//    // 时间戳
//    4:i64 timestamp;
//    // 通道
//    5:string channelCode;
//    // 状态码
//    6:string statusCode;
//    // 采集间隔
//    7:i32 interval;
//    // 请求次数
//    8:i64 requestCount;
//    // 发送失败数(包括业务返回失败，调用失败)
//    9:i64 failureCount;
//    // 响应时间
//    10:i64 costTime;
//    // 计费条数
//    11:i64 rechargeCount;
//    // 分类标识
//    12:optional string category;
//    // 通道名称
//    13:string channelName;
//}
//
///***************************************************************************/
///*****************************  Netty Metrics收集  *************************/
///***************************************************************************/
//
//struct NettyEntity {
//    // 应用名称
//    1:string appName;
//    // 主机地址
//    2:string hostName;
//    // 端口
//    3:i32 port;
//    // 时间戳
//    4:i64 timestamp;
//    // 采集时间间隔
//    5:i32 interval;
//    // channel信息
//    6:list<NettyPerformanceEntity> nettyPerformanceEntities;
//}
//
//struct NettyPerformanceEntity {
//    // 连接数
//    1:i32 connections;
//    // 接收方地址
//    2:string targetAddress;
//    // 发送完成数
//    3:i32 completedCount;
//    // 发送成功数
//    4:i32 successCount;
//    // 发送失败数
//    5:i32 cancelledCount;
//    // 发送消息总数
//    6:i32 sendCount;
//    // 发送异常数
//    7:i32 writtenExceptions;
//    // 接收异常数
//    8:i32 receivedExceptions;
//    // 发送字节数（单位：字节）
//    9:i64 writtenBytes;
//    // 接收字节数（单位：字节）
//    10:i64 receivedBytes;
//    // 发送耗时（单位：纳秒）
//    11:i64 costTime;
//}
//
///***************************************************************************/
///*****************************  业务自定义指标收集  *************************/
///***************************************************************************/
//struct KMetricsCounterEntity {
//    1:string groupName;
//    2:string metricsName;
//    3:i64 callCount;
//    4:i64 counter;
//}
//
//struct KMetricsTimerEntity {
//    1:string groupName;
//    2:string metricsName;
//    3:i64 callCount;
//    4:i64 elapsed;
//}
//
//struct KMetricsEntity {
//	// 应用名称
//    1:string appName;
//    // 主机地址
//    2:string hostName;
//    3:i32 port;
//    // 时间戳
//    4:i64 timestamp;
//    // 采集时间间隔
//    5:i32 interval;
//    // 计数器
//    6:list<KMetricsCounterEntity> counterEntities;
//    // 计时器
//    7:list<KMetricsTimerEntity> timerEntities;
//}
//
//
///***************************************************************************/
///*****************************  新Kafka Client数据采集Entity  *************************/
///***************************************************************************/
//struct KafkaConsumerCoordinatorEntity {
//    //consumer-coordinator-metrics
//    1:double assignedPartitions
//    2:double commitLatencyAvg
//    3:double commitLatencyMax
//    4:double commitRate
//    5:double heartbeatRate
//    6:double heartbeatResponseTimeMax
//    7:double joinRate
//    8:double joinTimeAvg
//    9:double joinTimeMax
//    10:double lastHeartbeatSecondsAgo
//    11:double syncRate
//    12:double syncTimeAvg
//    13:double syncTimeMax
//}
//
//struct KafkaConsumerFetcherEntity {
//    //consumer-fetch-manager-metrics
//    1:double bytesConsumedRate;
//    2:double fetchLatencyAvg;
//    3:double fetchLatencyMax;
//    4:double fetchRate;
//    5:double fetchSizeAvg;
//    6:double fetchSizeMax;
//    7:double fetchThrottleTimeAvg
//    8:double fetchThrottleTimeMax
//    9:double recordsConsumedRate
//    10:double recordsLagMax
//    11:double recordsPerRequestAvg
//}
//
//
//struct KafkaConsumerClientEntity {
//    1:string cluster;
//    2:string clientId;
//    3:string groupName;
//
//    4:KafkaConsumerCoordinatorEntity coordinatorEntity;
//    5:KafkaConsumerFetcherEntity fetcherEntity;
//
//    //consumer-metrics
//    6:double connectionCloseRate;
//    7:double connectionCount;
//    8:double connectionCreateRate;
//    9:double incomingByteRate;
//    10:double ioRatio;
//    11:double ioTimeNsAvg;
//    12:double ioWaitRatio;
//    13:double ioWaitTimeNsAvg;
//    14:double networkIoRate;
//    15:double outgoingByteRate;
//    16:double requestRate;
//    17:double requestSizeMax;
//    18:double requestSizeAvg;
//    19:double responseRate;
//    20:double selectRate;
//}
//
//struct KafkaConsumerTopicEntity {
//    1:string cluster;
//    2:string clientId;
//    3:string groupName;
//    4:string topic;
//
//    //consumer-fetch-manager-metrics
//    5:double fetchSizeAvg;
//    6:double fetchSizeMax;
//    7:double bytesConsumedRate;
//    8:double recordsPerRequestAvg;
//    9:double recordsConsumedRate;
//}
//
//struct KafkaProducerClientEntity {
//    1:string cluster;
//    2:string clientId;
//
//    //producer-metrics
//    3:double batchSizeAvg;
//    4:double batchSizeMax;
//    5:double bufferAvailableBytes;
//    6:double bufferExhaustedRate;
//    7:double bufferTotalBytes;
//    8:double bufferpoolWaitRatio;
//    9:double compressionRateAvg;
//    10:double metadataAge;
//    11:double produceThrottleTimeAvg;
//    12:double produceThrottleTimeMax;
//    13:double recordErrorRate;
//    14:double recordQueueTimeAvg;
//    15:double recordQueueTimeMax;
//    16:double recordRetryRate;
//    17:double recordSendRate;
//    18:double recordSizeAvg;
//    19:double recordSizeMax;
//    20:double recordsPerRequestAvg;
//    21:double requestLatencyAvg;
//    22:double requestLatencyMax;
//    23:double requestsInFlight;
//    24:double waitingThreads;
//    25:double connectionCloseRate;
//    26:double connectionCount;
//    27:double connectionCreationRate;
//    28:double incomingByteRate;
//    29:double ioRatio;
//    30:double ioTimeNsAvg;
//    31:double ioWaitRatio;
//    32:double ioWaitTimeNsAvg;
//    33:double networkIoRate;
//    34:double outgoingByteRate;
//    35:double requestRate;
//    36:double requestSizeMax;
//    37:double requestSizeAvg;
//    38:double responseRate;
//    39:double selectRate;
//}
//
//
//struct KafkaProducerTopicEntity {
//    1:string cluster;
//    2:string clientId;
//    3:string topic;
//
//    //producer-topic-metrics
//    4:double byteRate;
//    5:double compressionRate;
//    6:double recordErrorRate;
//    7:double recordRetryRate;
//    8:double recordSendRate;
//}
//
//struct KafkaClientNodeNetworkEntities {
//    1:string cluster;
//    2:string clientId;
//    3:string connectionId;
//
//    4:double incomingByteRate;
//    5:double outgoingByteRate;
//    6:double requestLatencyAvg;
//    7:double requestLatencyMax;
//    8:double requestRate;
//    9:double requestSizeAvg;
//    10:double requestSizeMax;
//    11:double responseRate;
//}
//
//struct KafkaClientEntity {
//	// 应用名称
//    1:string appName;
//    // 主机地址
//    2:string hostName;
//    3:i32 port;
//    // 时间戳
//    4:i64 timestamp;
//    // 采集时间间隔
//    5:i32 interval;
//    // consumer client metrics
//    6:list<KafkaConsumerClientEntity> consumerEntities;
//    // consumer topic metrics
//    7:list<KafkaConsumerTopicEntity> consumerTopicEntities;
//    // consumer network metrics, consumer-node-metrics
//    8:list<KafkaClientNodeNetworkEntities> consumerNetworkEntities;
//    // producer client metrics
//    9:list<KafkaProducerClientEntity> producerEntities;
//    // producer topic metrics
//    10:list<KafkaProducerTopicEntity> producerTopicEntities;
//    // producer network metrics, producer-node-metrics
//    11:list<KafkaClientNodeNetworkEntities> producerNodeNetworkEntities;
//}
//
//struct Properties {
//    1:string id;
//    2:string type;
//    3:map<string, string> properties;
//}
//
//struct ClientPropertiesEntity {
//	// 应用名称
//    1:string appName;
//    // 主机地址
//    2:string hostName;
//    3:i32 port;
//    // 时间戳
//    4:i64 timestamp;
//    // 采集时间间隔
//    5:i32 interval;
//    // client properties
//    6:list<Properties> properties;
//}
//
///***************************************************************************/
///*****************************  业务自定义跟踪收集  *************************/
///***************************************************************************/
//enum CompressionType {
//    NONE = 0,
//    LZ4 = 1
//}
//
//struct TraceLogEntity {
//    // 上报时间
//    1:i64 timestamp;
//    2:CompressionType compression;
//    3:string log;
//}
//
//struct TraceLogEntities {
//    // 应用名称
//    1:string appName;
//    // 主机地址
//    2:string hostName;
//    3:i32 port;
//
//    5:list<TraceLogEntity> logs;
//}
//
