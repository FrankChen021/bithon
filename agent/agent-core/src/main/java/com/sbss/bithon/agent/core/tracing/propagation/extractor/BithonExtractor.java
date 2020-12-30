package com.sbss.bithon.agent.core.tracing.propagation.extractor;

import com.sbss.bithon.agent.core.tracing.Tracer;
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.propagation.ITracePropagator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 10:00 上午
 */
public class BithonExtractor implements ITraceContextExtractor {

    @Override
    public <R> TraceContext extract(R request, PropagationGetter<R> getter) {
        if (request == null) {
            return null;
        }

        String traceId = getter.get(request, ITracePropagator.BITHON_TRACE_ID);
        if (traceId == null) {
            return null;
        }

        String spanIds = getter.get(request, ITracePropagator.BITHON_SPAN_IDS);
        if (spanIds == null) {
            return null;
        }

        String[] ids = spanIds.split(ITracePropagator.BITHON_ID_SEPARATOR);
        if (ids.length != 2) {
            return null;
        }

        TraceContext context = new TraceContext(traceId,
                                                ids[0],
                                                ids[1],
                                                Tracer.get().reporter(),
                                                Tracer.get().traceIdGenerator());
        context.currentSpan()
            .parentApplication(getter.get(request, ITracePropagator.BITHON_SOURCE_APPLICATION));
        return context;
    }
}
