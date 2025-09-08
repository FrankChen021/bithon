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
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.plugin.bithon.sdk.tracing.SpanScopeImpl;
import org.bithon.agent.sdk.expt.SdkException;
import org.bithon.agent.sdk.tracing.ISpanScope;
import org.bithon.agent.sdk.tracing.SpanKind;
import org.bithon.agent.sdk.tracing.SpanScopeBuilder;

/**
 * Interceptor for {@link SpanScopeBuilder#create()} method
 *
 * @author frank.chen021@outlook.com
 * @date 2025/01/20
 */
public class SpanScopeBuilder$Create extends ReplaceInterceptor {

    @Override
    public Object execute(Object thisObject, Object[] args, Object returning) {
        SpanScopeBuilder builder = (SpanScopeBuilder) thisObject;

        if (builder.operationName() == null) {
            throw new SdkException("Operation name is not set");
        }

        ITraceSpan span = TraceContextFactory.newSpan(builder.operationName());
        if (span == null) {
            return ISpanScope.NOOP_INSTANCE;
        }

        // Set span name and kind based on builder configuration
        span.name(builder.operationName());
        
        SpanKind sdkKind = builder.kind();
        if (sdkKind == SpanKind.CLIENT) {
            span.kind(org.bithon.component.commons.tracing.SpanKind.CLIENT);
        } else if (sdkKind == SpanKind.SERVER) {
            span.kind(org.bithon.component.commons.tracing.SpanKind.SERVER);
        } else if (sdkKind == SpanKind.PRODUCER) {
            span.kind(org.bithon.component.commons.tracing.SpanKind.PRODUCER);
        } else if (sdkKind == SpanKind.CONSUMER) {
            span.kind(org.bithon.component.commons.tracing.SpanKind.CONSUMER);
        } else if (sdkKind == SpanKind.INTERNAL) {
            span.kind(org.bithon.component.commons.tracing.SpanKind.INTERNAL);
        }

        return new SpanScopeImpl(span.start());
    }
}
