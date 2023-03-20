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

package org.bithon.agent.plugin.httpclient.netty3;

import org.bithon.agent.bootstrap.aop.context.AopContext;
import org.bithon.agent.bootstrap.aop.interceptor.AroundInterceptor;
import org.bithon.agent.bootstrap.aop.interceptor.InterceptionDecision;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetricsRegistry;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.StringUtils;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.HttpRequest;

/**
 * interceptor of {@link org.jboss.netty.channel.Channels#write(Channel, Object)}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/5/13 5:32 下午
 */
public class Channels$Write extends AroundInterceptor {

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();

    @Override
    public InterceptionDecision before(AopContext aopContext) throws Exception {
        if (!(aopContext.getArgs()[1] instanceof HttpRequest)) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        HttpRequest httpRequest = (HttpRequest) aopContext.getArgs()[1];

        final ITraceSpan span = TraceSpanFactory.newAsyncSpan("httpClient-netty3")
                                                .method(aopContext.getMethod())
                                                .kind(SpanKind.CLIENT)
                                                .tag(Tags.HTTP_METHOD, httpRequest.getMethod().getName())
                                                .propagate(
                                                    httpRequest.headers(),
                                                    (headersArgs, key, value) -> headersArgs.set(key, value)
                                                )
                                                .start();
        //
        // propagate tracing after span creation
        //
        if (span.isNull()) {
            return InterceptionDecision.CONTINUE;
        }

        aopContext.setUserContext(span);

        return super.before(aopContext);
    }

    @Override
    public void after(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        final HttpRequest httpRequest = (HttpRequest) aopContext.getArgs()[1];
        final String method = httpRequest.getMethod().getName();
        final long startAt = aopContext.getStartNanoTime();
        final ITraceSpan span = (ITraceSpan) aopContext.getUserContext();
        final String uri = getUri(httpRequest);

        // unlink reference
        aopContext.setUserContext(null);

        Object ret = aopContext.getReturning();
        if (!(ret instanceof ChannelFuture)) {
            return;
        }
        ChannelFuture future = (ChannelFuture) ret;

        future.addListener(channelFuture -> {
            //
            // metrics
            //
            if (channelFuture.getCause() != null) {
                metricRegistry.addExceptionRequest(
                    uri,
                    method,
                    System.nanoTime() - startAt
                );
            } else {
                // TODO: it's a little bit complex to get response
                // see NettyHttpClient in druid to know how to get HttpResponse
                metricRegistry.addRequest(
                    uri,
                    method,
                    200,
                    System.nanoTime() - startAt
                );
            }

            //
            // tracing
            //
            if (span != null) {
                span.tag(channelFuture.getCause())
                    .tag(Tags.HTTP_URI, uri)
                    .finish();
                span.context().finish();
            }
        });
    }

    private String getUri(HttpRequest httpRequest) {
        String uri = httpRequest.getUri();
        if (!httpRequest.getUri().startsWith("http")) {
            String host = httpRequest.headers().get("HOST");
            if (StringUtils.hasText(host)) {
                uri = "http://" + host + httpRequest.getUri();
            }
        }
        return uri;
    }
}
