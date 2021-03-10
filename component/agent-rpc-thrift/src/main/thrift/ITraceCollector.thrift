namespace java com.sbss.bithon.agent.rpc.thrift.service.trace

include "HeaderMessages.thrift"

struct TraceSpanMessage {
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

service ITraceCollector {
    oneway void sendTrace(1:required HeaderMessages.MessageHeader header, 2:list<TraceSpanMessage> spans);
}
