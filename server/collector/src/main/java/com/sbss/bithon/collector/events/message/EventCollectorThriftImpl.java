package com.sbss.bithon.collector.events.message;

import com.sbss.bithon.agent.rpc.thrift.service.event.IEventCollector;
import com.sbss.bithon.agent.rpc.thrift.service.event.ThriftEventMessage;
import com.sbss.bithon.agent.rpc.thrift.service.event.ThriftMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 3:59 下午
 */
@Slf4j
@Service
public class EventCollectorThriftImpl implements IEventCollector.Iface {

    private final EventsMessageHandler messageHandler;

    public EventCollectorThriftImpl(EventsMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void sendEvent(ThriftEventMessage message) throws TException {
        EventMessage eventMessage = EventMessage.builder().appName(message.getAppName())
            .instanceName(message.getHostName() + ":" + message.getPort())
            .timestamp(message.getTimestamp())
            .type(message.getEventType())
            .args(message.getArguments())
            .build();
        messageHandler.submit(eventMessage);
    }
}
