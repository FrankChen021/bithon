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

package org.bithon.agent.plugin.spring.webmvc.controller;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.springframework.web.method.support.InvocableHandlerMethod;

/**
 * {@link org.springframework.web.method.support.InvocableHandlerMethod#doInvoke(Object...)}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/5/8 16:16
 */
public class InvocableHandlerMethod$DoInvoke extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceContextFactory.newSpan("spring-controller");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        InvocableHandlerMethod handler = aopContext.getTargetAs();
        aopContext.setSpan(span.method(handler.getMethod())
                               .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();
        span.tag(aopContext.getException()).finish();
    }
}
