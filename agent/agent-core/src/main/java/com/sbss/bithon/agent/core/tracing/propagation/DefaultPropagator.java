package com.sbss.bithon.agent.core.tracing.propagation;

import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.propagation.extractor.ChainedTraceContextExtractor;
import com.sbss.bithon.agent.core.tracing.propagation.extractor.PropagationGetter;
import com.sbss.bithon.agent.core.tracing.propagation.injector.DefaultTraceContextInjector;
import com.sbss.bithon.agent.core.tracing.propagation.injector.PropagationSetter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:56 下午
 */
public class DefaultPropagator implements ITracePropagator {

    @Override
    public <R> void inject(TraceContext context, R request, PropagationSetter<R> setter) {
        new DefaultTraceContextInjector().inject(context, request, setter);
    }

    @Override
    public <R> TraceContext extract(R request, PropagationGetter<R> getter) {
        return new ChainedTraceContextExtractor().extract(request, getter);
    }
}
