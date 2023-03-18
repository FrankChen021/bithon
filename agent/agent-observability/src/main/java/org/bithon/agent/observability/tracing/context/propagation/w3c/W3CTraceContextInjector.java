/*
 *    Copyright 2020 bithon.org
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

package org.bithon.agent.observability.tracing.context.propagation.w3c;

import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.propagation.ITraceContextInjector;
import org.bithon.agent.observability.tracing.context.propagation.ITracePropagator;
import org.bithon.agent.observability.tracing.context.propagation.PropagationSetter;
import org.bithon.agent.observability.tracing.context.propagation.TraceMode;
import org.bithon.component.commons.logging.LoggerFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 12:27 上午
 */
public class W3CTraceContextInjector implements ITraceContextInjector {

    @Override
    public <R> void inject(ITraceContext context, R request, PropagationSetter<R> setter) {
        try {
            setter.put(request,
                       W3CTraceContextHeader.TRACE_HEADER_PARENT,
                       formatTraceParent(context.traceMode(), context.traceId(), context.currentSpan().spanId()));

            setter.put(request,
                       ITracePropagator.TRACE_HEADER_SRC_APPLICATION,
                       Tracer.get().appName());
        } catch (Exception e) {
            LoggerFactory.getLogger(W3CTraceContextInjector.class).error("Exception when propagating trace", e);
        }
    }

    /**
     * https://www.w3.org/TR/trace-context/#trace-id
     * version-format   = trace-id "-" parent-id "-" trace-flags
     * trace-id         = 32HEXDIGLC  ; 16 bytes array identifier. All zeroes forbidden
     * parent-id        = 16HEXDIGLC  ; 8 bytes array identifier. All zeroes forbidden
     * trace-flags      = 2HEXDIGLC   ; 8 bit flags. 1 for sampled
     */
    private String formatTraceParent(TraceMode traceMode, String traceId, String parentId) {
        return "00-" + traceId + "-" + parentId + (traceMode == TraceMode.TRACE ? "-01" : "-10");
    }
}
