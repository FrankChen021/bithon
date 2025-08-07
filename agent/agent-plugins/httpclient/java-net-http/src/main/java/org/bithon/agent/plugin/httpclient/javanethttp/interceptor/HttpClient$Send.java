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

package org.bithon.agent.plugin.httpclient.javanethttp.interceptor;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.metric.domain.httpclient.HttpOutgoingMetricsRegistry;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.TraceMode;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

/**
 * Intercepts java.net.http.HttpClient.send() method
 * 
 * @author frank.chen021@outlook.com
 * @date 2024/12/19
 */
public class HttpClient$Send extends AroundInterceptor {

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();
    private final TraceConfig traceConfig = ConfigurationManager.getInstance().getConfig(TraceConfig.class);

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        HttpRequest request = aopContext.getArgAs(0);
        
        // Create context for metrics collection
        IBithonObject bithonObject = aopContext.getTargetAs();
        if (bithonObject.getInjectedObject() == null) {
            bithonObject.setInjectedObject(new HttpClientContext());
        }

        HttpClientContext context = (HttpClientContext) bithonObject.getInjectedObject();
        context.setUrl(request.uri().toString());
        context.setMethod(request.method());
        context.setRequestStartTime(System.nanoTime());

        // Start tracing span
        ITraceSpan span = TraceContextFactory.newSpan("http-client", request.headers(), (headers, key, value) -> {
            // HttpHeaders is immutable in java.net.http, so we can't add headers here
            // The trace headers should be added by the user before calling send()
        });
        
        if (span != null) {
            span.method(aopContext.getTargetClass(), aopContext.getMethod())
                .kind(SpanKind.CLIENT)
                .tag(Tags.Http.CLIENT, "java.net.http")
                .tag(Tags.Http.URL, context.getUrl())
                .tag(Tags.Http.METHOD, context.getMethod());

            // Add configured request headers to trace
            if (!traceConfig.getHeaders().getRequest().isEmpty()) {
                for (String headerName : traceConfig.getHeaders().getRequest()) {
                    Optional<String> headerValue = request.headers().firstValue(headerName);
                    if (headerValue.isPresent()) {
                        span.tag(Tags.Http.REQUEST_HEADER_PREFIX + headerName, headerValue.get());
                    }
                }
            }

            span.start();
        }
        
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        IBithonObject bithonObject = aopContext.getTargetAs();
        HttpClientContext context = (HttpClientContext) bithonObject.getInjectedObject();
        
        long duration = System.nanoTime() - context.getRequestStartTime();
        
        if (aopContext.hasException()) {
            // Record exception metrics
            metricRegistry.addExceptionRequest(context.getUrl(), context.getMethod(), duration);
        } else {
            // Extract response information
            HttpResponse<?> response = aopContext.getReturningAs();
            int statusCode = response.statusCode();
            context.setResponseCode(statusCode);

            // Calculate response size if possible
            long responseSize = 0;
            Optional<String> contentLength = response.headers().firstValue("content-length");
            if (contentLength.isPresent()) {
                try {
                    responseSize = Long.parseLong(contentLength.get());
                } catch (NumberFormatException ignored) {
                    // Unable to parse content-length
                }
            }
                            context.getReceiveBytes().update(responseSize);

            // Record success metrics
            metricRegistry.addRequest(context.getUrl(), context.getMethod(), statusCode, duration)
                         .updateIOMetrics(context.getSentBytes().get(), context.getReceiveBytes().get());

            // Add response headers to trace and finish span
            finishTraceSpan(response);
        }
    }

    private void finishTraceSpan(HttpResponse<?> response) {
        ITraceContext ctx = TraceContextHolder.current();
        if (ctx == null || !ctx.traceMode().equals(TraceMode.TRACING)) {
            return;
        }
        
        ITraceSpan span = ctx.currentSpan();

        // Add configured response headers to trace
        if (!traceConfig.getHeaders().getResponse().isEmpty()) {
            for (String headerName : traceConfig.getHeaders().getResponse()) {
                Optional<String> headerValue = response.headers().firstValue(headerName);
                if (headerValue.isPresent()) {
                    span.tag(Tags.Http.RESPONSE_HEADER_PREFIX + headerName, headerValue.get());
                }
            }
        }

        span.tag(Tags.Http.STATUS, Integer.toString(response.statusCode()))
            .finish();
    }
}
