namespace java com.sbss.bithon.agent.rpc.thrift.service.metric.message


struct WebRequestMetricMessage {
    1:i64 timestamp;
    2:i32 interval;
    3:optional string srcApplication;
    4:string uri;

    5:i64 minResponseTime;
    6:i64 responseTime;
    7:i64 maxResponseTime;
    8:i64 callCount;
    9:i64 errorCount;
    10:i64 count4xx;
    11:i64 count5xx;
    12:i64 requestBytes;
    13:i64 responseBytes;
}

/***************************************************************************/
/*************************** JVM Metrics ***********************************/
/***************************************************************************/
struct JvmMetricMessage {
    1:i64 timestamp;
    2:i32 interval;
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
    1:optional string gcName;
    2:i32 generation;
    3:i64 gcCount;
    4:i64 gcTime;
}

struct ThreadEntity {
    1:i64 peakThreads;
    2:i64 daemonThreads;
    3:i64 totalThreads;
    4:i64 activeThreads;
}

struct ClassEntity {
    1:i64 currentLoaded;
    2:i64 totalLoaded;
    3:i64 totalUnloaded;
}

struct MetaspaceEntity {
    1:i64 metaspaceCommitted;
    2:i64 metaspaceUsed;
}

/***************************************************************************/
/************************* Web Server Metrics ******************************/
/***************************************************************************/
struct WebServerMetricMessage {
    1:i32 interval;
    2:i64 timestamp;
    3:string type;
    4:i64 connectionCount;
    5:i64 maxConnections;
    6:i64 activeThreads;
    7:i64 maxThreads;
}

/***************************************************************************/
/***************************  Application Exception ************************/
/**************************************************************************/
struct ExceptionMetricMessage {
    1:i32 interval;
    2:i64 timestamp;
    3:string className;
    4:string message;
    5:string stackTrace;
    6:string uri;
    7:i64 exceptionCount;
}

/***************************************************************************/
/******************* Http Client *******************************************/
/**************************************************************************/
struct HttpClientMetricMessage {
    1:i64 timestamp;
    2:i32 interval;
    3:string uri;
    4:string method;
    5:i64 responseTime;
    6:i64 minResponseTime;
    7:i64 maxResponseTime;
    8:i64 count4xx;
    9:i64 count5xx;
    10:i64 requestCount;
    11:i64 exceptionCount;
    12:i64 requestBytes;
    13:i64 responseBytes;
}

/***************************************************************************/
/******************* Thread Pool ******************************************/
/**************************************************************************/
struct ThreadPoolMetricMessage {
    1:i64 timestamp;
    2:i32 interval;
    3:string executorClass;
    4:string poolName;
    5:i64 callerRunTaskCount;
    6:i64 abortedTaskCount;
    7:i64 discardedTaskCount;
    8:i64 discardedOldestTaskCount;
    9:i64 exceptionTaskCount;
    10:i64 successfulTaskCount;
    11:i64 totalTaskCount;
    12:i64 activeThreads;
    13:i64 currentPoolSize;
    14:i64 maxPoolSize;
    15:i64 largestPoolSize;
    16:i64 queuedTaskCount;
}

/***************************************************************************/
/*******************************  JDBC Pool      ***************************/
/***************************************************************************/
struct JdbcPoolMetricMessage {
    1:i64 timestamp;
    2:i32 interval;
    3:string driverClass;
    4:string connectionString;
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

/***************************************************************************/
/*******************************  SQL             ***************************/
/***************************************************************************/
struct SqlMetricMessage {
    1:string connectionString;
    2:i64 timestamp;
    3:i32 interval;
    4:i64 callCount;
    5:i64 responseTime;
    6:i64 minResponseTime;
    7:i64 maxResponseTime;
    8:i64 errorCount;
    9:i64 queryCount;
    10:i64 updateCount;
}

/***************************************************************************/
/*******************************  Redis          ***************************/
/***************************************************************************/
struct RedisMetricMessage {
    1:string uri;
    2:string command;
    3:i64 timestamp;
    4:i32 interval;
    5:i64 exceptionCount;
    6:i64 totalCount;
    7:i64 requestTime;
    8:i64 responseTime;
    9:i64 requestBytes;
    10:i64 responseBytes;
}
