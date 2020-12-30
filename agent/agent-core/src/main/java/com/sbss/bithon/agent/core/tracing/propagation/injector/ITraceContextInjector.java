package com.sbss.bithon.agent.core.tracing.propagation.injector;

import com.sbss.bithon.agent.core.tracing.context.TraceContext;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:39 下午
 */
public interface ITraceContextInjector {
    <R> void inject(TraceContext context, R request, PropagationSetter<R> setter);
}
