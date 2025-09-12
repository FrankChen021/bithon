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

package org.bithon.agent.plugin.bithon.sdk;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frank.chen021@outlook.com
 * @date 2021-10-01
 */
public class BithonSdkPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            //
            // metrics SDK
            //
            forClass("org.bithon.agent.sdk.metric.MetricRegistryFactory")
                .onMethod("create")
                .replacedBy("org.bithon.agent.plugin.bithon.sdk.interceptor.MetricRegistryFactory$Create")
                .build(),

            //
            // tracing SDK
            //
            forClass("org.bithon.agent.sdk.tracing.TraceContext")
                .onMethod("currentTraceId")
                .replacedBy("org.bithon.agent.plugin.bithon.sdk.tracing.interceptor.TraceContext$CurrentTraceId")

                .onMethod("currentSpanId")
                .replacedBy("org.bithon.agent.plugin.bithon.sdk.tracing.interceptor.TraceContext$CurrentSpanId")

                .onMethod("newScopedSpan")
                .andNoArgs()
                .replacedBy("org.bithon.agent.plugin.bithon.sdk.tracing.interceptor.TraceContext$NewSpan")

                .onMethod("currentSpan")
                .andNoArgs()
                .replacedBy("org.bithon.agent.plugin.bithon.sdk.tracing.interceptor.TraceContext$CurrentSpan")
                .build(),

            forClass("org.bithon.agent.sdk.tracing.TraceScopeBuilder")
                .onMethod("attach")
                .replacedBy("org.bithon.agent.plugin.bithon.sdk.tracing.interceptor.TraceScopeBuilder$Attach")

                .build(),

            forClass("org.bithon.agent.sdk.tracing.SpanScopeBuilder")
                .onMethod("create")
                .replacedBy("org.bithon.agent.plugin.bithon.sdk.tracing.interceptor.SpanScopeBuilder$Create")

                .build()
        );
    }
}
