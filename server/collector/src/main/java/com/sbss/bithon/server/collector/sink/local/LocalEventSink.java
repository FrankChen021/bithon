package com.sbss.bithon.server.collector.sink.local;

import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.events.collector.EventMessage;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/16
 */
public class LocalEventSink implements IMessageSink<EventMessage> {

    @Override
    public void process(String messageType, EventMessage message) {

    }
}
