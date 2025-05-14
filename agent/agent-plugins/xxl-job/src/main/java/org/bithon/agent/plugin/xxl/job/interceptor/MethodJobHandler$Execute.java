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

package org.bithon.agent.plugin.xxl.job.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.sampler.SamplingMode;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * {@link com.xxl.job.core.handler.impl.MethodJobHandler#execute()}
 *
 * @author frankchen
 */
public class MethodJobHandler$Execute extends AroundInterceptor {
    @Override
    public InterceptionDecision before(AopContext aopContext) {
        UserDefinedContext ctx = UserDefinedContext.getAndRemove();
        if (ctx == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ITraceContext traceContext = TraceContextFactory.newContext(SamplingMode.FULL, ctx.getTraceId(), ctx.getParentSpanId());
        TraceContextHolder.attach(traceContext);
        ITraceSpan span = traceContext.currentSpan()
                                      .name("method-job");

        Method targetMethod = (Method) ReflectionUtils.getFieldValue(aopContext.getTarget(), "method");
        aopContext.setSpan(span.method(targetMethod)
                               .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();
        span.tag(aopContext.getException())
            .finish();

        span.context().finish();

        TraceContextHolder.detach();
    }
}
