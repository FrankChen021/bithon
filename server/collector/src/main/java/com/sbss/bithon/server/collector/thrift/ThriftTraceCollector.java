package com.sbss.bithon.server.collector.thrift;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.trace.ITraceCollector;
import com.sbss.bithon.agent.rpc.thrift.service.trace.TraceSpanMessage;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.tracing.handler.TraceSpan;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/23 11:19 下午
 */
@Slf4j
public class ThriftTraceCollector implements ITraceCollector.Iface {

    private final IMessageSink<List<TraceSpan>> traceSink;

    public ThriftTraceCollector(IMessageSink<List<TraceSpan>> traceSink) {
        this.traceSink = traceSink;
    }

    @Override
    public void sendTrace(MessageHeader header, List<TraceSpanMessage> spans) {
        log.info("Receiving trace message:{}", spans);
        traceSink.process("trace", TraceSpan.from(header, spans));
    }
}
