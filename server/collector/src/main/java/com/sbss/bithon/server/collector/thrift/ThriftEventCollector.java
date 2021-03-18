package com.sbss.bithon.server.collector.thrift;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.event.IEventCollector;
import com.sbss.bithon.agent.rpc.thrift.service.event.ThriftEventMessage;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.event.handler.EventMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 3:59 下午
 */
@Slf4j
public class ThriftEventCollector implements IEventCollector.Iface {

    private final IMessageSink<EventMessage> eventSink;

    public ThriftEventCollector(IMessageSink<EventMessage> eventSink) {
        this.eventSink = eventSink;
    }

    @Override
    public void sendEvent(MessageHeader header, ThriftEventMessage message) {
        EventMessage eventMessage = EventMessage.builder().appName(header.getAppName())
                                                .instanceName(header.getInstanceName())
                                                .timestamp(message.getTimestamp())
                                                .type(message.getEventType())
                                                .args(message.getArguments())
                                                .build();
        eventSink.process("event", eventMessage);
    }
}
