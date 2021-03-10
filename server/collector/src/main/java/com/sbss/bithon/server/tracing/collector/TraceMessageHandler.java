package com.sbss.bithon.server.tracing.collector;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.trace.TraceSpanMessage;
import com.sbss.bithon.server.collector.AbstractThreadPoolMessageHandler;
import com.sbss.bithon.server.tracing.storage.ITraceStorage;
import com.sbss.bithon.server.tracing.storage.ITraceWriter;
import com.sbss.bithon.server.tracing.storage.TraceSpan;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:21 下午
 */
@Component
public class TraceMessageHandler extends AbstractThreadPoolMessageHandler<MessageHeader, List<TraceSpanMessage>> {

    final ITraceWriter traceWriter;

    public TraceMessageHandler(ITraceStorage traceStorage) {
        super(2,
              10,
              Duration.ofMinutes(1),
              2048);

        this.traceWriter = traceStorage.createWriter();
    }

    @Override
    protected void onMessage(MessageHeader header,
                             List<TraceSpanMessage> traceSpans) throws IOException {
        traceWriter.write(TraceSpan.from(header, traceSpans));
    }
}
