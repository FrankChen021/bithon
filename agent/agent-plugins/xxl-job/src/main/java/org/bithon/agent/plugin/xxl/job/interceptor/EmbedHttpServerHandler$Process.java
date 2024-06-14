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

import com.xxl.job.core.biz.model.ReturnT;
import io.netty.handler.codec.http.HttpMethod;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.sampler.SamplingMode;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

/**
 * {@link com.xxl.job.core.server.EmbedServer.EmbedHttpServerHandler#process(HttpMethod httpMethod, String uri, String requestData, String accessTokenReq)}
 *
 * @author Frank Chen
 * @date 23/2/24 1:39 pm
 */
public class EmbedHttpServerHandler$Process extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        HttpMethod method = aopContext.getArgAs(0);
        String uri = aopContext.getArgAs(1);

        // Currently no samplingPercentage is designed for simplicity
        TraceContextHolder.attach(TraceContextFactory.newContext(SamplingMode.FULL))
                          .currentSpan()
                          .component("xxl-job")
                          .kind(SpanKind.SERVER)
                          .method(aopContext.getTargetClass(), aopContext.getMethod())
                          .tag(Tags.Http.METHOD, method.name())
                          .tag(Tags.Http.URL, uri)
                          .start();

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceContext context = TraceContextHolder.current();
        if (context == null) {
            return;
        }

        // TODO: We can merge the remove and get above together as one for simplicity
        TraceContextHolder.detach();

        ITraceSpan span = context.currentSpan();
        Object returning = aopContext.getReturning();
        if (returning instanceof ReturnT) {
            int code = ((ReturnT<?>) returning).getCode();
            if (code != ReturnT.SUCCESS_CODE) {
                span.tag("message", ((ReturnT<?>) returning).getMsg());
            }
        }
        span.tag(aopContext.getException())
            .finish();

        context.finish();
    }
}
