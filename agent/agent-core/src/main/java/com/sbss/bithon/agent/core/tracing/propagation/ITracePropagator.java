package com.sbss.bithon.agent.core.tracing.propagation;

import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.propagation.extractor.PropagationGetter;
import com.sbss.bithon.agent.core.tracing.propagation.injector.PropagationSetter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:36 下午
 */
public interface ITracePropagator {

    String BITHON_SOURCE_APPLICATION = "BITHON-SOURCE-APP";
    String BITHON_TRACE_ID = "BITHON-TRACE-ID";
    String BITHON_SPAN_IDS = "BITHON-SPAN-IDS";
    String BITHON_ID_SEPARATOR = ";";

    <R> void inject(TraceContext context, R request, PropagationSetter<R> setter);

    <R> TraceContext extract(R request, PropagationGetter<R> getter);
}
