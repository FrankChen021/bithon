package com.sbss.bithon.server.tracing.storage;

import com.sbss.bithon.server.tracing.handler.TraceSpan;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 3:28 下午
 */
public interface ITraceReader {
    List<TraceSpan> getTraceByTraceId(String traceId);

    List<TraceSpan> getTraceList(String appName, int pageNumber, int pageSize);
    int getTraceListSize(String appName);

    List<TraceSpan> getTraceByParentSpanId(String parentSpanId);
}
