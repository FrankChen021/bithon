package com.sbss.bithon.server.collector.sink.local;

import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.tracing.storage.TraceSpan;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/16
 */
public class LocalTraceSink implements IMessageSink<List<TraceSpan>> {
    @Override
    public void process(String messageType, List<TraceSpan> message) {

    }
}
