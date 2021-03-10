namespace java com.sbss.bithon.agent.rpc.thrift.service.trace

struct TraceSpan {
    6:string traceId;
    7:string spanId;
    8:string parentSpanId;
    9:string parentAppName;
    10:string kind;
    11:string name;
    12:string clazz;
    13:string method;
    14:map<string, string> tags;
    15:i64 startTime;
    16:i64 endTime;
}

struct TraceMessage {
    1:string appName;
    2:string env;
    3:string hostName;
    4:i32 port;
    5:i64 timestamp;

    6:list<TraceSpan> spans;
}

service ITraceCollector {
    oneway void sendTrace(1:required TraceMessage message);
}
