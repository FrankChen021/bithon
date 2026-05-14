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

package org.bithon.agent.plugin.spring.webmvc7.rs;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.component.commons.tracing.Tags;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Spring 7-compatible {@link org.springframework.web.client.RestTemplate#handleResponse}.
 *
 * @author frankchen
 */
public class RestTemplate$HandleResponse7 extends BeforeInterceptor {

    @Override
    public void before(AopContext aopContext) throws Exception {
        ITraceContext context = TraceContextHolder.current();
        if (context == null) {
            return;
        }

        ITraceSpan span = context.currentSpan();
        if (span == null) {
            return;
        }

        ClientHttpResponse response = aopContext.getArgAs(2);
        span.tag(Tags.Http.STATUS, String.valueOf(response.getStatusCode().value()));
    }
}
