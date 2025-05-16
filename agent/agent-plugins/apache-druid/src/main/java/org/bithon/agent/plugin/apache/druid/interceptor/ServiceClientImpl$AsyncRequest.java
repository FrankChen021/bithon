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

package org.bithon.agent.plugin.apache.druid.interceptor;

import org.apache.druid.rpc.RequestBuilder;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.impl.TracingContext;

/**
 * Since Druid 24
 * {@link org.apache.druid.rpc.ServiceClientImpl#asyncRequest(RequestBuilder, org.apache.druid.java.util.http.client.response.HttpResponseHandler)}
 *
 * It's very complicated to set up trace span around this async request, as a compromise, we only propagate trace context.
 *
 * @author frank.chen021@outlook.com
 * @date 2025/1/18 18:37
 */
public class ServiceClientImpl$AsyncRequest extends BeforeInterceptor {
    @Override
    public void before(AopContext aopContext) {
        ITraceContext context = TraceContextHolder.current();
        if (context instanceof TracingContext) {
            RequestBuilder requestBuilder = aopContext.getArgAs(0);
            context.propagate(requestBuilder, RequestBuilder::header);
        }
    }
}
