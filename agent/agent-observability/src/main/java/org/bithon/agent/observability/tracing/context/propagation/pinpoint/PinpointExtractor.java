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

package org.bithon.agent.observability.tracing.context.propagation.pinpoint;

import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.propagation.ITraceContextExtractor;
import org.bithon.agent.observability.tracing.context.propagation.PropagationGetter;
import org.bithon.agent.observability.tracing.id.ISpanIdGenerator;
import org.bithon.agent.observability.tracing.id.impl.DefaultSpanIdGenerator;
import org.bithon.agent.observability.tracing.id.impl.SpanIdGenerator;
import org.bithon.agent.observability.tracing.sampler.SamplingMode;
import org.bithon.component.commons.utils.StringUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 9:39 上午
 */
public class PinpointExtractor implements ITraceContextExtractor {

    /**
     * src/main/java/com/navercorp/pinpoint/bootstrap/context/Header.java
     */
    public static final String TRACE_ID = "Pinpoint-TraceID";
    public static final String SPAN_ID = "Pinpoint-SpanID";
    public static final String PARENT_SPAN_ID = "Pinpoint-pSpanID";
    public static final String SAMPLED = "Pinpoint-Sampled";
    public static final String APP_NAME = "Pinpoint-pAppName";

    @Override
    public <R> ITraceContext extract(R request, PropagationGetter<R> getter) {
        String traceId = getter.get(request, TRACE_ID);
        if (StringUtils.isEmpty(traceId)) {
            return null;
        }

        String parentSpanId = getter.get(request, PARENT_SPAN_ID);
        if (StringUtils.isEmpty(parentSpanId)) {
            return null;
        }

        String spanId = getter.get(request, SPAN_ID);
        if (StringUtils.isEmpty(spanId)) {
            spanId = Tracer.get().spanIdGenerator().newSpanId();
        }

        return TraceContextFactory.newContext(SamplingMode.FULL,
                                              traceId,
                                              parentSpanId,
                                              spanId,
                                              Tracer.get().spanIdGenerator());
    }
}
