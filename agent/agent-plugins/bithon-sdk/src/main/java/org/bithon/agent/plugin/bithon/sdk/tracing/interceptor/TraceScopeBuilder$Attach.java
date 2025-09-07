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

package org.bithon.agent.plugin.bithon.sdk.tracing.interceptor;

import org.bithon.agent.instrumentation.aop.interceptor.declaration.ReplaceInterceptor;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.sampler.SamplingMode;
import org.bithon.agent.plugin.bithon.sdk.tracing.TraceScopeImpl;
import org.bithon.agent.sdk.tracing.TraceScopeBuilder;
import org.bithon.agent.sdk.tracing.TracingMode;
import org.bithon.component.commons.utils.StringUtils;

/**
 * Interceptor for {@link TraceScopeBuilder#attach()} methods
 *
 * @author frank.chen021@outlook.com
 * @date 8/5/25 6:15 pm
 */
public class TraceScopeBuilder$Attach extends ReplaceInterceptor {

    @Override
    public Object execute(Object[] args, Object returning) {
        // For instance methods, the target object is passed as the first argument
        TraceScopeBuilder builder = (TraceScopeBuilder) args[0];
        boolean startSpan = args.length > 1 ? (Boolean) args[1] : true;

        String traceId = builder.getTraceId();
        String parentSpanId = builder.getParentSpanId();
        if (StringUtils.isEmpty(traceId) || StringUtils.isEmpty(parentSpanId)) {
            traceId = Tracer.get().traceIdGenerator().newId();
            parentSpanId = "";
        }

        SamplingMode samplingMode = builder.getTracingMode() == TracingMode.TRACING ? SamplingMode.FULL : SamplingMode.NONE;
        ITraceContext ctx = TraceContextFactory.newContext(samplingMode, traceId, parentSpanId);
        ctx.currentSpan().name(builder.getOperationName());

        //noinspection resource
        return new TraceScopeImpl(ctx, ctx.currentSpan()).attach(startSpan);
    }
}
