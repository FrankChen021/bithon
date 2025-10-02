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

package org.bithon.agent.plugin.httpclient.reactor.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.plugin.httpclient.reactor.HttpClientContext;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.reactivestreams.Publisher;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClientRequest;

import java.util.function.BiFunction;

/**
 * See {@link reactor.netty.http.client.HttpClientFinalizer#send}
 *
 * @author frank.chen021@outlook.com
 * @date 27/11/21 1:57 pm
 */
public class HttpClientFinalizer$Send extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        IBithonObject bithonObject = aopContext.getTargetAs();

        // span will be finished in ResponseConnection interceptor,
        // so we need an extra object to pass the context,
        // which should be setup in the uri()/request()
        HttpClientContext httpClientContext = (HttpClientContext) bithonObject.getInjectedObject();
        if (httpClientContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ITraceSpan span = TraceContextFactory.newAsyncSpan("http-client");
        if (span != null) {
            // Span will be finished in ResponseConnection interceptor
            // If the Span is null, we still need to continue because the response connection also records metrics
            //
            httpClientContext.setSpan(span);
        }

        //noinspection unchecked
        BiFunction<? super HttpClientRequest, ? super NettyOutbound, ? extends Publisher<Void>> originalSender
            = (BiFunction<? super HttpClientRequest, ? super NettyOutbound, ? extends Publisher<Void>>) aopContext.getArgs()[0];

        // Replace the Publisher to propagate trace
        aopContext.getArgs()[0] = (BiFunction<HttpClientRequest, NettyOutbound, Publisher<Void>>) (httpClientRequest, nettyOutbound) -> {
            // Update the start time
            httpClientContext.setStartTimeNs(System.nanoTime());

            // Propagate trace along this HTTP
            if (span != null) {
                span.kind(SpanKind.CLIENT)
                    .method(aopContext.getTargetClass(), aopContext.getMethod())
                    .tag(Tags.Http.URL, httpClientContext.getUri())
                    .tag(Tags.Http.METHOD, httpClientContext.getMethod())
                    .tag(Tags.Http.CLIENT, "reactor")
                    .context().propagate(httpClientRequest, (request, key, value) -> request.requestHeaders().set(key, value));
                span.start();
            }

            return originalSender.apply(httpClientRequest, nettyOutbound);
        };

        return InterceptionDecision.CONTINUE;
    }

    /**
     * target method returns a new copy, so we have to pass the trace span to the new copy for further processing (such as ResponseConnection)
     */
    @Override
    public void after(AopContext aopContext) {
        IBithonObject currentHttpClient = aopContext.getTargetAs();
        IBithonObject newHttpClient = aopContext.getReturningAs();
        newHttpClient.setInjectedObject(currentHttpClient.getInjectedObject());
    }
}
