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

package org.bithon.agent.plugin.spring.webflux.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetricsRegistry;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.plugin.spring.webflux.context.HttpClientContext;
import org.bithon.component.commons.tracing.Tags;
import org.reactivestreams.Publisher;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.util.function.BiFunction;

/**
 * see reactor.netty.http.client.HttpClientFinalizer#responseConnection
 *
 * @author frank.chen021@outlook.com
 * @date 27/11/21 1:57 pm
 */
public class HttpClientFinalizer$ResponseConnection extends AroundInterceptor {

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        HttpClient httpClient = aopContext.getTargetAs();
        String uri = httpClient.configuration().uri();
        String method = httpClient.configuration().method().name();

        // injected by HttpClientFinalizer$Send's onMethodLeave
        IBithonObject bithonObject = aopContext.getTargetAs();
        HttpClientContext httpClientContext = (HttpClientContext) bithonObject.getInjectedObject();

        //noinspection unchecked,rawtypes
        BiFunction<? super HttpClientResponse, ? super Connection, ? extends Publisher> originalReceiver
            = (BiFunction<? super HttpClientResponse, ? super Connection, ? extends Publisher>) aopContext.getArgs()[0];

        //
        //replace original publisher so that we can stop the span
        //
        //noinspection rawtypes
        aopContext.getArgs()[0] = (BiFunction<HttpClientResponse, Connection, Publisher>) (httpClientResponse, connection) -> {
            //noinspection rawtypes
            Publisher publisher = originalReceiver.apply(httpClientResponse, connection);

            try {
                // tracing
                final ITraceSpan httpClientSpan = httpClientContext.getSpan();
                if (httpClientSpan != null) {
                    httpClientSpan.tag(Tags.Http.STATUS, String.valueOf(httpClientResponse.status().code()))
                                  .tag(Tags.HTTP_URI, uri)
                                  .finish();
                }

                // metrics
                metricRegistry.addRequest(uri,
                                          method,
                                          httpClientResponse.status().code(),
                                          System.nanoTime() - httpClientContext.getStartTimeNs());
            } catch (Exception ignored) {
            }

            return publisher;
        };

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        HttpClient httpClient = aopContext.getTargetAs();
        String uri = httpClient.configuration().uri();
        String method = httpClient.configuration().method().name();

        IBithonObject bithonObject = aopContext.getTargetAs();

        // injected by HttpClientFinalizer$Send's onMethodLeave
        HttpClientContext httpClientContext = (HttpClientContext) bithonObject.getInjectedObject();

        Flux<?> responseFlux = aopContext.getReturningAs();

        /**
         * Hook on exception
         *
         * NOTE:
         *  we don't hook on complete instead we do the final thing in the replaced response handler injected above
         *  because the runtime reports the response on the doOnNext is illegal accessed
         */
        Flux<?> replacedReturning = responseFlux.doOnError((throwable -> {
            Integer statusCode = null;
            if (throwable instanceof ResponseStatusException) {
                statusCode = ((ResponseStatusException) throwable).getStatus().value();
            }

            // tracing
            final ITraceSpan httpClientSpan = httpClientContext.getSpan();
            if (httpClientSpan != null) {
                if (statusCode != null) {
                    httpClientSpan.tag(Tags.Http.STATUS, String.valueOf(statusCode));
                }
                httpClientSpan.tag(throwable)
                              .tag(Tags.HTTP_URI, uri)
                              .finish();
            }

            // metrics
            if (statusCode == null) {
                metricRegistry.addExceptionRequest(uri,
                                                   method,
                                                   System.nanoTime() - httpClientContext.getStartTimeNs());
            } else {
                metricRegistry.addRequest(uri,
                                          method,
                                          statusCode,
                                          System.nanoTime() - httpClientContext.getStartTimeNs());
            }
        }));

        // post process of Flux.timeout
        ((IBithonObject) replacedReturning).setInjectedObject((Runnable) () -> {
            // tracing
            final ITraceSpan httpClientSpan = httpClientContext.getSpan();
            if (httpClientSpan != null) {
                httpClientSpan.tag("exception", "Timeout")
                              .tag(Tags.HTTP_URI, uri)
                              .finish();
            }

            metricRegistry.addExceptionRequest(uri,
                                               method,
                                               System.nanoTime() - httpClientContext.getStartTimeNs());
        });
        aopContext.setReturning(replacedReturning);
    }
}
