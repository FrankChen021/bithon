package com.sbss.bithon.agent.core.tracing.report;

import com.sbss.bithon.agent.core.tracing.context.TraceSpan;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:45 下午
 */
public interface ITraceReporter {
    void report(List<TraceSpan> spans);
}
