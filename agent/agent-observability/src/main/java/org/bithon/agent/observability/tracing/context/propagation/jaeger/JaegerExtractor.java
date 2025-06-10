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

package org.bithon.agent.observability.tracing.context.propagation.jaeger;

import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.propagation.ITraceContextExtractor;
import org.bithon.agent.observability.tracing.context.propagation.PropagationGetter;
import org.bithon.agent.observability.tracing.sampler.SamplingMode;
import org.bithon.component.commons.utils.StringUtils;

/**
 * Jaeger trace context extractor that supports Uber's original trace propagation format.
 * <p>
 * The Jaeger format uses the "uber-trace-id" header with the format:
 * {trace-id}:{span-id}:{parent-span-id}:{flags}
 *
 * @author frank.chen021@outlook.com
 * @date 2025/05/26
 */
public class JaegerExtractor implements ITraceContextExtractor {

    /**
     * Jaeger's native trace context header name
     */
    public static final String UBER_TRACE_ID = "uber-trace-id";

    // Bit flags for Jaeger flags field.
    private static final int SAMPLED_FLAG = 0x01;
    private static final int DEBUG_FLAG = 0x02;

    @Override
    public <R> ITraceContext extract(R request, PropagationGetter<R> getter) {
        if (request == null) {
            return null;
        }

        String uberTraceId = getter.get(request, UBER_TRACE_ID);
        if (StringUtils.isEmpty(uberTraceId)) {
            return null;
        }

        // Parse the uber-trace-id format: {trace-id}:{span-id}:{parent-span-id}:{flags}
        String[] parts = uberTraceId.split(":");
        if (parts.length != 4) {
            return null;
        }

        String traceId = parts[0];

        // For us, the SPAN_ID is the parent span ID of the new span in this service
        String parentSpanId = parts[1];
        String flags = parts[3];

        // Validate trace-id
        if (StringUtils.isEmpty(traceId) || StringUtils.isEmpty(parentSpanId)) {
            return null;
        }

        // Parse flags to determine sampling mode
        SamplingMode samplingMode = getSamplingMode(flags);
        return TraceContextFactory.newContext(samplingMode,
                                              traceId,
                                              parentSpanId,
                                              Tracer.get().spanIdGenerator());
    }

    /**
     * Parse Jaeger flags to determine sampling mode.
     * Bit 1 (least significant bit, mask 0x01) is the "sampled" flag.
     * Bit 2 (mask 0x02) is the "debug" flag.
     */
    private SamplingMode getSamplingMode(String flag) {
        if (StringUtils.isEmpty(flag)) {
            return SamplingMode.NONE;
        }

        try {
            int flags = Integer.parseInt(flag, 16);

            // The debug flag imposes a sampling decision.
            if ((flags & DEBUG_FLAG) == DEBUG_FLAG) {
                return SamplingMode.FULL;
            }

            // Check if the sampled flag (bit 1) is set
            return (flags & SAMPLED_FLAG) == SAMPLED_FLAG ? SamplingMode.FULL : SamplingMode.NONE;
        } catch (NumberFormatException e) {
            // If flags cannot be parsed, default to not sampled
            return SamplingMode.NONE;
        }
    }
}
