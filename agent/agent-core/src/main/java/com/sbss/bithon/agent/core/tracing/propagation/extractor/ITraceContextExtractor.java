package com.sbss.bithon.agent.core.tracing.propagation.extractor;

import com.sbss.bithon.agent.core.tracing.context.TraceContext;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:39 下午
 */
public interface ITraceContextExtractor {
    <R> TraceContext extract(R request, PropagationGetter<R> getter);
}
