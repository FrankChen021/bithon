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

package org.bithon.agent.core.tracing.context;

import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.core.tracing.Tracer;
import org.bithon.agent.core.tracing.propagation.TraceMode;

import java.util.regex.Pattern;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/5 18:04
 */
public class TraceContextFactory {
    static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-zA-Z]{32}");

    public static ITraceContext create(TraceMode traceMode, String traceId) {
        return create(traceMode, traceId, null);
    }

    public static ITraceContext create(TraceMode traceMode, String traceId, String parentSpanId) {
        return create(traceMode,
                      traceId,
                      parentSpanId,
                      Tracer.get().spanIdGenerator().newSpanId());
    }

    public static ITraceContext create(TraceMode traceMode, String traceId, String parentSpanId, String spanId) {
        //
        // check compatibility of trace id
        //
        String upstreamTraceId = null;
        if (traceId.length() != 32 || !UUID_PATTERN.matcher(traceId).matches()) {
            upstreamTraceId = traceId;
            traceId = Tracer.get().traceIdGenerator().newTraceId();
        }

        //
        // create trace context
        //
        ITraceContext context;
        switch (traceMode) {
            case TRACE:
                context = new TraceContext(traceId, Tracer.get().spanIdGenerator()).reporter(Tracer.get().reporter());
                break;
            case PROPAGATION:
                context = new PropagationTraceContext(traceId, Tracer.get().spanIdGenerator());
                break;
            default:
                // actually never happen
                // just a branch to make the code readable
                throw new AgentException("Unknown trace mode:%s", traceMode);
        }

        //
        // set necessary status
        //
        ITraceSpan span = context.newSpan(parentSpanId, spanId);
        if (upstreamTraceId != null) {
            span.tag("upstreamTraceId", upstreamTraceId);
        }
        return context;
    }
}
