package com.sbss.bithon.server.collector.kafka;

import com.sbss.bithon.server.collector.sink.local.LocalEventSink;
import com.sbss.bithon.server.event.handler.EventMessage;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public class KafkaEventCollector extends AbstractKafkaCollector<EventMessage> {
    private final LocalEventSink localSink;

    public KafkaEventCollector() {
        super(EventMessage.class);
        localSink = new LocalEventSink();
    }

    @Override
    protected String getGroupId() {
        return "bithon-collector-event";
    }

    @Override
    protected String[] getTopics() {
        return new String[]{"event"};
    }

    @Override
    protected void onMessage(String topic, EventMessage eventMessage) {
        localSink.process(topic, eventMessage);
    }
}
