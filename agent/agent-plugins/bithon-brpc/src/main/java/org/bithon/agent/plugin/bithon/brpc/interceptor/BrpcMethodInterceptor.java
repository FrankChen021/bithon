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

package org.bithon.agent.plugin.bithon.brpc.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.Tracer;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.TraceContextFactory;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.core.tracing.propagation.TraceMode;
import org.bithon.agent.core.tracing.sampler.SamplingMode;

/**
 * @author Frank Chen
 * @date 12/5/22 10:25 PM
 */
public class BrpcMethodInterceptor extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        ITraceContext context;
        SamplingMode mode = Tracer.get().sampler().decideSamplingMode(null);
        if (mode == SamplingMode.NONE) {
            // create a propagation trace context to propagation trace context along the service call without reporting trace data
            context = TraceContextFactory.create(TraceMode.PROPAGATION,
                                                 "P-" + Tracer.get().traceIdGenerator().newTraceId());
        } else {
            // create a traceable context
            context = TraceContextFactory.create(TraceMode.TRACE,
                                                 Tracer.get().traceIdGenerator().newTraceId());
        }

        aopContext.setUserContext(context.currentSpan()
                                         .component("brpc")
                                         .kind(SpanKind.SERVER)
                                         .method(aopContext.getMethod())
                                         .tag("uri", aopContext.getTarget().getClass().getSimpleName() + "#" + aopContext.getMethod().getName())
                                         .start());

        TraceContextHolder.set(context);
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        ITraceSpan span = aopContext.castUserContextAs();
        span.tag(aopContext.getException())
            .tag("status", aopContext.hasException() ? "500" : "200")
            .finish();
        span.context().finish();

        TraceContextHolder.remove();
    }
}
