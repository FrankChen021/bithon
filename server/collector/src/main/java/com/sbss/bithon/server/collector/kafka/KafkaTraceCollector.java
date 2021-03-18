package com.sbss.bithon.server.collector.kafka;

import com.sbss.bithon.server.collector.sink.local.LocalTraceSink;
import com.sbss.bithon.server.tracing.handler.TraceMessageHandler;
import com.sbss.bithon.server.tracing.handler.TraceSpan;

import java.util.ArrayList;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public class KafkaTraceCollector extends AbstractKafkaCollector<TraceMessage> {
    private final LocalTraceSink localSink;

    public KafkaTraceCollector(TraceMessageHandler traceMessageHandler) {
        super(TraceMessage.class);

        localSink = new LocalTraceSink(traceMessageHandler);
    }

    @Override
    protected String getGroupId() {
        return "bithon-collector-trace";
    }

    @Override
    protected String[] getTopics() {
        return new String[]{"trace"};
    }

    @Override
    protected void onMessage(String topic, TraceMessage traceSpans) {
        localSink.process(topic, traceSpans);
    }
}

class TraceMessage extends ArrayList<TraceSpan> {
}
