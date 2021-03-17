package com.sbss.bithon.agent.core.tracing.propagation.injector;

import com.sbss.bithon.agent.core.tracing.Tracer;
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.propagation.ITracePropagator;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 12:27 上午
 */
public class DefaultTraceContextInjector implements ITraceContextInjector {

    @Override
    public <R> void inject(TraceContext context, R request, PropagationSetter<R> setter) {
        try {
            setter.put(request, ITracePropagator.BITHON_TRACE_ID, context.traceId());
            setter.put(request,
                       ITracePropagator.BITHON_SPAN_IDS,
                       context.spanIdGenerator().newSpanId()
                       + ITracePropagator.BITHON_ID_SEPARATOR
                       + context.currentSpan().spanId());
            setter.put(request,
                       ITracePropagator.BITHON_SOURCE_APPLICATION,
                       Tracer.get().appName());
        } catch (Exception e) {
            LoggerFactory.getLogger(DefaultTraceContextInjector.class).error("Exception when propagating trace", e);
        }
    }
}
