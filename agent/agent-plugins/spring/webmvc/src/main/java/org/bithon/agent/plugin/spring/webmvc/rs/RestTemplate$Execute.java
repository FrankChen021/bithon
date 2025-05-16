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

package org.bithon.agent.plugin.spring.webmvc.rs;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.springframework.http.HttpMethod;

/**
 * {@link org.springframework.web.client.RestTemplate#doExecute}
 *
 * @author frankchen
 * @date 2021-02-16 14:36
 */
public class RestTemplate$Execute extends AroundInterceptor {
    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceContextFactory.newSpan("spring");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        String uri = aopContext.getArgs()[0].toString();

        String method;
        if (aopContext.getArgs().length == 5) {
            // SpringBoot 3.x
            method = ((HttpMethod) aopContext.getArgs()[2]).name();
        } else {
            // SpringBoot 2.x
            method = ((HttpMethod) aopContext.getArgs()[1]).name();
        }

        aopContext.setSpan(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                               // Set the kind to CLIENT in case the underlying lib is not instrumented
                               .kind(SpanKind.CLIENT)
                               .tag(Tags.Http.METHOD, method)
                               .tag(Tags.Http.URL, uri)
                               .start());

        return InterceptionDecision.CONTINUE;
    }


    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();
        span.tag(aopContext.getException())

            .finish();
    }
}
