package com.sbss.bithon.server.collector.sink.local;

import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.tracing.handler.TraceMessageHandler;
import com.sbss.bithon.server.tracing.handler.TraceSpan;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/16
 */
public class LocalTraceSink implements IMessageSink<List<TraceSpan>> {

    private final TraceMessageHandler traceMessageHandler;

    public LocalTraceSink(TraceMessageHandler traceMessageHandler) {
        this.traceMessageHandler = traceMessageHandler;
    }

    @Override
    public void process(String messageType, List<TraceSpan> message) {
        traceMessageHandler.submit(message);
    }
}
