package com.sbss.bithon.agent.core.tracing.propagation.extractor;

import com.sbss.bithon.agent.core.tracing.Tracer;
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.propagation.ITracePropagator;
import com.sbss.bithon.agent.core.tracing.sampling.SamplingMode;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:59 下午
 */
public class ChainedTraceContextExtractor implements ITraceContextExtractor {

    private final ITraceContextExtractor[] extractors = new ITraceContextExtractor[]{
        new B3Extractor(),
        new BithonExtractor(),
        };

    @Override
    public <R> TraceContext extract(R request, PropagationGetter<R> getter) {
        //
        // TODO: sampling decision making first
        //
        SamplingMode mode = Tracer.get().samplingDecisionMaker().decideSamplingMode(request);
        if (mode == SamplingMode.NONE) {
            return null;
        }

        for (ITraceContextExtractor extractor : extractors) {
            TraceContext context = extractor.extract(request, getter);
            if (context != null) {
                return context.samplingMode(mode);
            }
        }

        // context decide by SamplingMode
        TraceContext context = new TraceContext(Tracer.get().traceIdGenerator().newTraceId(),
                                                Tracer.get().reporter(),
                                                Tracer.get().traceIdGenerator());
        context.currentSpan()
               .parentApplication(getter.get(request, ITracePropagator.BITHON_SOURCE_APPLICATION));
        return context;
    }

}