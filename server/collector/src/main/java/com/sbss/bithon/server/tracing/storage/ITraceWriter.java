package com.sbss.bithon.server.tracing.storage;

import java.io.IOException;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:25 下午
 */
public interface ITraceWriter extends AutoCloseable {

    void write(List<TraceSpan> traceSpans) throws IOException;
}
