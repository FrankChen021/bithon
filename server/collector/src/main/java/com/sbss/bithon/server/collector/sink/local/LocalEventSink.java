package com.sbss.bithon.server.collector.sink.local;

import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.event.handler.EventMessage;
import com.sbss.bithon.server.event.handler.EventsMessageHandler;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/16
 */
public class LocalEventSink implements IMessageSink<EventMessage> {

    private final EventsMessageHandler handler;

    public LocalEventSink(EventsMessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void process(String messageType, EventMessage message) {
        this.handler.submit(message);
    }
}
