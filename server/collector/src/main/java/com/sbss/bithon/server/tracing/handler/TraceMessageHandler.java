package com.sbss.bithon.server.tracing.handler;

import com.sbss.bithon.server.common.handler.AbstractThreadPoolMessageHandler;
import com.sbss.bithon.server.tracing.storage.ITraceStorage;
import com.sbss.bithon.server.tracing.storage.ITraceWriter;
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
        super("trace",
              2,
              10,
              Duration.ofMinutes(1),
              2048);

        this.traceWriter = traceStorage.createWriter();
    }

    @Override
    protected void onMessage(List<TraceSpan> traceSpans) throws IOException {
        traceWriter.write(traceSpans);
    }

    @Override
    public String getType() {
        return "trace";
    }
}
