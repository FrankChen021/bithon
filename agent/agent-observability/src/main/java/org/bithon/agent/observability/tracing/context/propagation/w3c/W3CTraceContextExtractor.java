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
import org.bithon.agent.observability.tracing.context.TraceContextAttributes;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.propagation.ITraceContextExtractor;
import org.bithon.agent.observability.tracing.context.propagation.ITracePropagator;
import org.bithon.agent.observability.tracing.context.propagation.PropagationGetter;
import org.bithon.agent.observability.tracing.sampler.SamplingMode;
import org.bithon.component.commons.utils.StringUtils;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 10:00 上午
 */
public class W3CTraceContextExtractor implements ITraceContextExtractor {

    private static final int SAMPLED_FLAG = 0x1;

    @Override
    public <R> ITraceContext extract(R request, PropagationGetter<R> getter) {
        if (request == null) {
            return null;
        }

        String traceParent = getter.get(request, W3CTraceContextHeader.TRACE_HEADER_PARENT);
        if (traceParent == null) {
            return null;
        }

        List<String> ids = StringUtils.split(traceParent, "-");
        if (ids.size() != 4) {
            return null;
        }

        // version
        if (ids.get(0).length() != 2) {
            return null;
        }

        // traceId
        String traceId = ids.get(1);
        if (traceId.length() != 32 || !StringUtils.isHexString(traceId)) {
            return null;
        }

        // parent span id
        String parentSpanId = ids.get(2);
        if (parentSpanId.length() != 16 || !StringUtils.isHexString(parentSpanId)) {
            return null;
        }

        // trace flags
        String traceFlags = ids.get(3);
        if (traceFlags.length() != 2 || !StringUtils.isHexString(traceFlags)) {
            return null;
        }

        String traceState = getter.get(request, W3CTraceContextHeader.TRACE_HEADER_STATE);
        TraceContextAttributes attributes = TraceContextAttributes.deserialize(traceState);

        return TraceContextFactory.newContext(getSamplingMode(traceFlags),
                                              traceId,
                                              parentSpanId,
                                              Tracer.get().spanIdGenerator())
                                  .attribute(attributes)
                                  .currentSpan()
                                  .parentApplication(getter.get(request, ITracePropagator.TRACE_HEADER_SRC_APPLICATION))
                                  .context();
    }

    private SamplingMode getSamplingMode(String traceFlags) {
        int flag = traceFlags.charAt(0) - '0' * 16 + (traceFlags.charAt(1) - '0');
        return (flag & SAMPLED_FLAG) == SAMPLED_FLAG ? SamplingMode.FULL : SamplingMode.NONE;
    }
}
