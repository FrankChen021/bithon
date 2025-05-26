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
        String spanId = parts[1];
        String parentSpanId = parts[2];
        String flags = parts[3];

        // Validate trace-id (must not be empty or all zeros)
        if (StringUtils.isEmpty(traceId) || isAllZeros(traceId)) {
            return null;
        }

        // Validate span-id (must not be empty or all zeros)
        if (StringUtils.isEmpty(spanId) || isAllZeros(spanId)) {
            return null;
        }

        // Parent span ID can be 0 for root spans, so we don't validate it as strictly
        if (StringUtils.isEmpty(parentSpanId)) {
            return null;
        }

        // If parent span ID is all zeros (but not single "0"), treat it as empty string (root span)
        if (isAllZeros(parentSpanId) && parentSpanId.length() > 1) {
            parentSpanId = "";
        }

        // Parse flags to determine sampling mode
        SamplingMode samplingMode = getSamplingMode(flags);

        return TraceContextFactory.newContext(samplingMode,
                                              traceId,
                                              parentSpanId,
                                              spanId,
                                              Tracer.get().spanIdGenerator());
    }

    /**
     * Check if a hex string is all zeros
     */
    private boolean isAllZeros(String hexString) {
        if (StringUtils.isEmpty(hexString)) {
            return true;
        }
        for (char c : hexString.toCharArray()) {
            if (c != '0') {
                return false;
            }
        }
        return true;
    }

    /**
     * Parse Jaeger flags to determine sampling mode.
     * Bit 1 (least significant bit, mask 0x01) is the "sampled" flag.
     */
    private SamplingMode getSamplingMode(String flag) {
        if (StringUtils.isEmpty(flag)) {
            return SamplingMode.NONE;
        }

        try {
            int flags = Integer.parseInt(flag, 16);
            // Check if the sampled flag (bit 1) is set
            return (flags & 0x01) == 0x01 ? SamplingMode.FULL : SamplingMode.NONE;
        } catch (NumberFormatException e) {
            // If flags cannot be parsed, default to not sampled
            return SamplingMode.NONE;
        }
    }
}
