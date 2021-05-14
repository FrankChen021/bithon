/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.httpclient.jetty;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import com.sbss.bithon.agent.core.tracing.context.TraceSpanBuilder;
import org.eclipse.jetty.client.HttpRequest;
import org.eclipse.jetty.client.api.Response;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/5/13 7:56 下午
 */
public class HttpRequestInterceptor extends AbstractInterceptor {

    /**
     * {@link org.eclipse.jetty.client.HttpRequest#send(Response.CompleteListener)}
     *
     * @param aopContext
     * @return
     */
    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {

        final TraceSpan span = TraceSpanBuilder.build("httpClient-jetty")
                                               .method(aopContext.getMethod())
                                               .kind(SpanKind.CLIENT)
                                               .start();

        if (span.isNull()) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        //
        // propagate tracing after span creation
        //
        HttpRequest httpRequest = aopContext.castTargetAs();
        span.context().propagate(httpRequest.getHeaders(), (headersArgs, key, value) -> {
            headersArgs.put(key, value);
        });

        // replace listener
        final Response.CompleteListener rawListener = (Response.CompleteListener) aopContext.getArgs()[0];
        aopContext.getArgs()[0] = (Response.CompleteListener) result -> {
            if (result.isFailed()) {
                if (result.getRequestFailure() != null) {
                    span.tag(result.getRequestFailure());
                }
                if (result.getResponseFailure() != null) {
                    span.tag(result.getResponseFailure());
                }
            }
            span.finish();
            rawListener.onComplete(result);
        };

        return super.onMethodEnter(aopContext);
    }

}
