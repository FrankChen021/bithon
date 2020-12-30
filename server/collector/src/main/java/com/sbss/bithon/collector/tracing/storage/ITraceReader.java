package com.sbss.bithon.collector.tracing.storage;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 3:28 下午
 */
public interface ITraceReader {
    List<TraceSpan> getTraceByTraceId(String traceId);

    List<TraceSpan> getTraceList(String appName);

    List<TraceSpan> getTraceByParentSpanId(String parentSpanId);
}
