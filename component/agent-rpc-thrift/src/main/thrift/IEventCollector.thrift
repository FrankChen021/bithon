namespace java com.sbss.bithon.agent.rpc.thrift.service.event

include "HeaderMessages.thrift"

struct ThriftEventMessage {
    1:i64 timestamp;
    2:string eventType;
    3:map<string,string> arguments;
}

service IEventCollector {
    oneway void sendEvent(1:required HeaderMessages.MessageHeader header,
                          2:required ThriftEventMessage body);
}
