namespace java com.sbss.bithon.agent.rpc.thrift.service.event

struct ThriftEventMessage {
    1:string appName;
    2:string env;
    3:string hostName;
    4:i32 port;
    5:i64 timestamp;
    6:string eventType;
    7:map<string,string> arguments;
}

service IEventCollector {
    oneway void sendEvent(1:required ThriftEventMessage message);
}
