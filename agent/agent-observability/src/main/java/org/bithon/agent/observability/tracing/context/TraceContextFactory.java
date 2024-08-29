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

package org.bithon.agent.observability.tracing.context;

import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.context.impl.LoggingTraceContext;
import org.bithon.agent.observability.tracing.context.impl.TracingContext;
import org.bithon.agent.observability.tracing.context.propagation.PropagationSetter;
import org.bithon.agent.observability.tracing.id.ISpanIdGenerator;
import org.bithon.agent.observability.tracing.id.impl.SpanIdGenerator;
import org.bithon.agent.observability.tracing.sampler.SamplingMode;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.StringUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/5 18:04
 */
public class TraceContextFactory {

    public static ITraceContext newContext(SamplingMode samplingMode) {
        return newContext(samplingMode, Tracer.get().traceIdGenerator().newId(), null, new SpanIdGenerator(0, 0));
    }

    public static ITraceContext newContext(SamplingMode samplingMode,
                                           String traceId,
                                           String parentSpanId,
                                           ISpanIdGenerator spanIdGenerator) {
        return newContext(samplingMode, traceId, parentSpanId, spanIdGenerator.newSpanId(), spanIdGenerator);
    }

    public static ITraceContext newContext(SamplingMode samplingMode,
                                           String traceId,
                                           String parentSpanId,
                                           String spanId,
                                           ISpanIdGenerator spanIdGenerator) {
        //
        // check compatibility of trace id
        //
        String upstreamTraceId = null;
        if (traceId.length() != 32 || !StringUtils.isHexString(traceId)) {
            upstreamTraceId = traceId;
            traceId = Tracer.get().traceIdGenerator().newId();
        }

        //
        // Create trace context
        //
        ITraceContext context;
        switch (samplingMode) {
            case FULL:
                context = new TracingContext(traceId, spanIdGenerator).reporter(Tracer.get().reporter());
                break;
            case NONE:
                context = new LoggingTraceContext(traceId, spanIdGenerator);
                break;
            default:
                // actually never happen
                // just a branch to make the code readable
                throw new AgentException("Unknown sampling mode:%s", samplingMode);
        }

        Thread currentThread = Thread.currentThread();
        return context.newSpan(parentSpanId, spanId)
                      .tag(Tags.Thread.NAME, currentThread.getName())
                      .tag(Tags.Thread.ID, currentThread.getId())
                      .tag("upstreamTraceId", upstreamTraceId)
                      .context();
    }

    /**
     * Create a span based on current span on current thread
     */
    public static ITraceSpan newSpan(String name) {
        return newSpan(name, null, null);
    }

    public static <T> ITraceSpan newSpan(String name, T injectedTo, PropagationSetter<T> setter) {
        ITraceContext traceContext = TraceContextHolder.current();
        if (traceContext == null) {
            return null;
        }

        if (traceContext.traceMode().equals(TraceMode.LOGGING)) {
            // No need to create SPAN under logging mode but only propagate context
            if (injectedTo != null && setter != null) {
                traceContext.propagate(injectedTo, setter);
            }
            return null;
        }

        ITraceSpan parentSpan = traceContext.currentSpan();
        if (parentSpan == null) {
            return null;
        }

        // create a span and save it in user-context
        ITraceSpan span = parentSpan.newChildSpan(name);
        if (injectedTo != null && setter != null) {
            traceContext.propagate(injectedTo, setter);
        }
        return span;
    }

    /**
     * This method copies the current trace context so that it can be used in another thread.
     * Even current trace mode is {@link TraceMode#LOGGING}, it's still copied.
     */
    public static ITraceSpan newAsyncSpan(String name) {
        return newAsyncSpan(name, null, null);
    }

    public static <T> ITraceSpan newAsyncSpan(String name, T injectedTo, PropagationSetter<T> setter) {
        ITraceContext traceContext = TraceContextHolder.current();
        if (traceContext == null) {
            return null;
        }

        ITraceSpan parentSpan = traceContext.currentSpan();
        if (parentSpan == null) {
            return null;
        }

        ITraceSpan span = traceContext.copy()
                                      .reporter(traceContext.reporter())
                                      .newSpan(parentSpan.spanId(), traceContext.spanIdGenerator().newSpanId())
                                      .component(name);
        if (injectedTo != null && setter != null) {
            span.context().propagate(injectedTo, setter);
        }
        return span;
    }
}
