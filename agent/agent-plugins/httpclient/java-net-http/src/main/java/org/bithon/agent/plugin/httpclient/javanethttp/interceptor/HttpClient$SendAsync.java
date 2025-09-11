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
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * {@link jdk.internal.net.http.HttpClientImpl#sendAsync(HttpRequest, HttpResponse.BodyHandler, HttpResponse.PushPromiseHandler, Executor)}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/19
 */
public class HttpClient$SendAsync extends AroundInterceptor {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(HttpClient$SendAsync.class);

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();
    private final TraceConfig traceConfig = ConfigurationManager.getInstance().getConfig(TraceConfig.class);

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        IBithonObject bithonObject = aopContext.getInjectedOnTargetAs();
        if (bithonObject.getInjectedObject() != null) {
            // the sendAsync may be called by the 'send' method internally
            return InterceptionDecision.SKIP_LEAVE;
        }

        HttpRequest request = aopContext.getArgAs(0);

        // Start tracing span
        ITraceSpan span = TraceContextFactory.newAsyncSpan("http-client");
        if (span != null) {
            span.method(aopContext.getTargetClass(), aopContext.getMethod())
                .kind(SpanKind.CLIENT)
                .tag(Tags.Http.CLIENT, "java.net.http")
                .configIfTrue(!traceConfig.getHeaders().getRequest().isEmpty(), (s) -> {
                    for (String headerName : traceConfig.getHeaders().getRequest()) {
                        request.headers()
                               .firstValue(headerName)
                               .ifPresent(val -> s.tag(Tags.Http.REQUEST_HEADER_PREFIX + headerName, val));
                    }
                });

            aopContext.setUserContext(span.start());
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        HttpRequest request = aopContext.getArgAs(0);
        final String url = request.uri().toString();
        final String httpMethod = request.method();

        final ITraceSpan span = aopContext.getUserContext();
        if (span != null) {
            span.tag(Tags.Http.URL, url)
                .tag(Tags.Http.METHOD, httpMethod);
        }

        if (aopContext.hasException()) {
            metricRegistry.addExceptionRequest(url,
                                               httpMethod,
                                               aopContext.getExecutionTime());

            // Finish span with exception
            if (span != null) {
                span.finish();
            }
            return;
        }

        // For async operations, we need to hook into the returned CompletableFuture
        CompletableFuture<HttpResponse<?>> returnFuture = aopContext.getReturningAs();

        // Wrap the returned CompletableFuture to handle completion/exception
        final long startNanoTime = aopContext.getStartNanoTime();
        CompletableFuture<HttpResponse<?>> wrappedFuture = returnFuture.whenComplete((response, throwable) -> {
            try {
                long duration = System.nanoTime() - startNanoTime;

                if (throwable != null) {
                    // Record exception metrics
                    metricRegistry.addExceptionRequest(url, httpMethod, duration);
                } else {
                    // Extract response information
                    int statusCode = response.statusCode();

                    // Calculate response size if possible
                    long responseSize = getResponseSize(response);

                    // Record success metrics
                    metricRegistry.addRequest(url, httpMethod, statusCode, duration)
                                  .updateIOMetrics(getRequestSize(request), getResponseSize(response));

                    // Add response headers to trace
                    if (span != null) {
                        if (traceConfig.getHeaders() != null) {
                            for (String headerName : traceConfig.getHeaders().getResponse()) {
                                Optional<String> headerValue = response.headers().firstValue(headerName);
                                headerValue.ifPresent(s -> span.tag(Tags.Http.RESPONSE_HEADER_PREFIX + headerName, s));
                            }
                        }

                        span.tag(Tags.Http.STATUS, Integer.toString(statusCode));
                    }
                }

                // Finish span
                if (span != null) {
                    span.tag(throwable)
                        .finish();
                }
            } catch (Throwable t) {
                // Catch all to prevent any exception from thrown from agent to user code
                LOG.warn("Unexpected error in HttpClient$SendAsync", t);
            }
        });

        // Replace the original future with our wrapped one
        aopContext.setReturning(wrappedFuture);
    }

    private long getRequestSize(HttpRequest request) {
        Optional<String> contentLength = request.headers().firstValue("content-length");
        if (contentLength.isPresent()) {
            try {
                return Long.parseLong(contentLength.get());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private long getResponseSize(HttpResponse<?> response) {
        Optional<String> contentLength = response.headers().firstValue("content-length");
        if (contentLength.isPresent()) {
            try {
                return Long.parseLong(contentLength.get());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }
}
