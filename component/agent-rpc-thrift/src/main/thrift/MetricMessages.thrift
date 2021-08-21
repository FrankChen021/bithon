namespace java com.sbss.bithon.agent.rpc.thrift.service.metric.message


struct HttpIncomingMetricMessage {
    1:i64 timestamp;
    2:i32 interval;
    3:optional string srcApplication;
    4:string uri;

    5:i64 minResponseTime;
    6:i64 responseTime;
    7:i64 maxResponseTime;
    8:i64 callCount;
    9:i64 errorCount;
    10:i64 okCount;
    11:i64 count4xx;
    12:i64 count5xx;
    13:i64 requestBytes;
    14:i64 responseBytes;
    15:i64 flowedCount;
    16:i64 degradedCount;
}

/***************************************************************************/
/*************************** JVM Metrics ***********************************/
/***************************************************************************/
struct JvmMetricMessage {
    1:i64 timestamp;
    2:i32 interval;

    3:i64 instanceUpTime      // how long the application run in millisecond
    4:i64 instanceStartTime;  // start timestamp in milli second
    5:i64 processors;
    6:i64 processCpuTime;     // process cpu time in this interval in nano second
    7:double systemLoadAvg;
    8:double processCpuLoad;

    9:i64 totalMemBytes;
    10:i64 freeMemBytes;

    11:i64 heapMax;     // approximately to -Xmx
    12:i64 heapInit;    // approximate to -Xms
    13:i64 heapUsed;
    14:i64 heapCommitted;

    //
    // heap
    //
    15:i64 nonHeapMax;  // -XX:MaxPermSize
    16:i64 nonHeapInit; // -XX:PermSize
    17:i64 nonHeapUsed;
    18:i64 nonHeapCommitted;

    //
    // threads
    //
    19:i64 peakThreads;
    20:i64 daemonThreads;
    21:i64 totalThreads;
    22:i64 activeThreads;

    //
    // class
    //
    23:i64 classLoaded;
    24:i64 classUnloaded;

    //
    // meta
    //
    25:i64 metaspaceCommitted;
    26:i64 metaspaceUsed;
    27:i64 metaspaceInit;
    28:i64 metaspaceMax;

    29:i64 directMax;
    30:i64 directUsed;
}

struct JvmGcMetricMessage {
    1:i64 timestamp;
    2:i32 interval;
    3:string gcName;
    4:string generation;
    5:i64 gcCount;
    6:i64 gcTime;
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
struct HttpOutgoingMetricMessage {
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

/**************************************************************************/
/*******************************  MongoDb       ***************************/
/**************************************************************************/
struct MongoDbMetricMessage {
    1:string server;
    2:string database;
    3:string collection;
    4:string command;
    5:i64 timestamp;
    6:i32 interval;

    7:i64 callCount;
    8:i64 exceptionCount;
    9:i64 responseTime;
    10:i64 minResponseTime;
    11:i64 maxResponseTime;
    12:i64 requestBytes;
    13:i64 responseBytes;
}