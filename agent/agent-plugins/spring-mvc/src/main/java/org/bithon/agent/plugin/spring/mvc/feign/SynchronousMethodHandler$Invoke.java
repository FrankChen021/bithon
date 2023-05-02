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

package org.bithon.agent.plugin.spring.mvc.feign;


import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;
import org.bithon.component.commons.tracing.SpanKind;

/**
 * {@link feign.SynchronousMethodHandler#invoke(Object[])}
 *
 * @author frankchen
 * @date 2021-02-16 14:41
 */
public class SynchronousMethodHandler$Invoke extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        if (!(aopContext.getTarget() instanceof IBithonObject)) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        IBithonObject methodHandler = aopContext.getTargetAs();
        FeignInvocationContext invocationContext = (FeignInvocationContext) methodHandler.getInjectedObject();
        if (invocationContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ITraceSpan span = TraceSpanFactory.newSpan("feignClient");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setUserContext(span.kind(SpanKind.CLIENT)
                                      .method(invocationContext.methodMeta.method())
                                      .tag("target", invocationContext.target.url())
                                      .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getUserContextAs();

        span.tag(aopContext.getException())
            .finish();
    }
}
