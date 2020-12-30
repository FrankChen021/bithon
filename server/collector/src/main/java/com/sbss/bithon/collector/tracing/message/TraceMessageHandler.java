package com.sbss.bithon.collector.tracing.message;

import com.sbss.bithon.collector.common.message.handlers.AbstractThreadPoolMessageHandler;
import com.sbss.bithon.collector.tracing.storage.ITraceStorage;
import com.sbss.bithon.collector.tracing.storage.ITraceWriter;
import com.sbss.bithon.collector.tracing.storage.TraceSpan;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:21 下午
 */
@Component
public class TraceMessageHandler extends AbstractThreadPoolMessageHandler<List<TraceSpan>> {

    final ITraceWriter traceWriter;

    public TraceMessageHandler(ITraceStorage traceStorage) {
        super(2,
              10,
              Duration.ofMinutes(1),
              2048);

        this.traceWriter = traceStorage.createWriter();
    }

    @Override
    protected void onMessage(List<TraceSpan> traceSpans) throws IOException {
        traceWriter.write(traceSpans);
    }
}
