/*
 *    Copyright 2020 bithon.cn
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.agent.core.tracing.propagation.injector;

import org.bithon.agent.core.tracing.Tracer;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.propagation.ITracePropagator;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 12:27 上午
 */
public class DefaultTraceContextInjector implements ITraceContextInjector {

    @Override
    public <R> void inject(ITraceContext context, R request, PropagationSetter<R> setter) {
        try {
            setter.put(request, ITracePropagator.BITHON_TRACE_ID, context.traceId());

            // propagate currentSpanId(parent for the next):nextSpanId
            setter.put(request,
                       ITracePropagator.BITHON_SPAN_IDS,
                       context.currentSpan().spanId()
                       + ITracePropagator.BITHON_ID_SEPARATOR
                       + context.spanIdGenerator().newSpanId()
            );

            setter.put(request,
                       ITracePropagator.BITHON_SRC_APPLICATION,
                       Tracer.get().appName());
        } catch (Exception e) {
            LoggerFactory.getLogger(DefaultTraceContextInjector.class).error("Exception when propagating trace", e);
        }
    }
}
