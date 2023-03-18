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

import io.netty.handler.codec.http.HttpHeaders;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.config.ConfigurationManager;
import org.bithon.agent.observability.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.observability.metric.domain.web.HttpIncomingMetricsRegistry;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.propagation.ITracePropagator;
import org.bithon.agent.observability.tracing.context.propagation.TraceMode;
import org.bithon.agent.plugin.spring.webflux.config.ResponseConfigs;
import org.bithon.agent.plugin.spring.webflux.context.HttpServerContext;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * {@link org.springframework.http.server.reactive.ReactorHttpHandlerAdapter#apply}
 *
 * @author frank.chen021@outlook.com
 * @date 7/10/21 3:16 pm
 */
public class ReactorHttpHandlerAdapter$Apply extends AbstractInterceptor {

    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ReactorHttpHandlerAdapter$Apply.class);
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final HttpIncomingMetricsRegistry metricRegistry = HttpIncomingMetricsRegistry.get();
    private HttpIncomingFilter requestFilter;
    private TraceConfig traceConfig;
    private ResponseConfigs responseConfigs;
    private String xforwardTagName;

    @Override
    public boolean initialize() {
        requestFilter = new HttpIncomingFilter();

        traceConfig = ConfigurationManager.getInstance().getConfig(TraceConfig.class);
        responseConfigs = ConfigurationManager.getInstance().getConfig(ResponseConfigs.class);

        // remove the special header for fast processing later
        xforwardTagName = responseConfigs.getHeaders().remove(X_FORWARDED_FOR);

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        final HttpServerRequest request = (HttpServerRequest) aopContext.getArgs()[0];

        boolean shouldExclude = requestFilter.shouldBeExcluded(request.uri(),
                                                               request.requestHeaders().get("User-Agent"));
        if (shouldExclude) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        TraceContextHolder.remove();

        //
        // HttpServerRequest has an implementation of HttpServerOperation
        // which is instrumented as IBithonObject
        //
        if (request instanceof IBithonObject) {
            // the injected object is created in HttpServerOperation$Ctor
            Object injected = ((IBithonObject) request).getInjectedObject();
            if (injected instanceof HttpServerContext) {
                final ITraceContext traceContext = Tracer.get()
                                                         .propagator()
                                                         .extract(request, (req, key) -> req.requestHeaders().get(key));

                if (traceContext != null) {

                    traceContext.currentSpan()
                                .component("webflux")
                                .tag(Tags.REMOTE_ADDR, request.remoteAddress())
                                .tag(Tags.HTTP_URI, request.uri())
                                .tag(Tags.HTTP_METHOD, request.method().name())
                                .tag(Tags.HTTP_VERSION, request.version().text())
                                .tag((span) -> traceConfig.getHeaders()
                                                          .getRequest()
                                                          .forEach((header) -> span.tag("http.header." + header, request.requestHeaders().get(header))))
                                .method(aopContext.getMethod())
                                .kind(SpanKind.SERVER)
                                .start();

                    // put the trace id in the header so that the applications have chance to know whether this request is being sampled
                    if (traceContext.traceMode().equals(TraceMode.TRACE)) {
                        request.requestHeaders().set("X-Bithon-TraceId", traceContext.traceId());

                        // Add trace id to response
                        String traceIdHeader = traceConfig.getTraceIdInResponse();
                        if (StringUtils.hasText(traceIdHeader)) {
                            final HttpServerResponse response = aopContext.getArgAs(1);
                            response.addHeader(traceIdHeader, traceContext.traceId());
                        }
                    }

                    ((HttpServerContext) injected).setTraceContext(traceContext);
                }
            }
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        final HttpServerRequest request = (HttpServerRequest) aopContext.getArgs()[0];
        final HttpServerResponse response = (HttpServerResponse) aopContext.getArgs()[1];

        Mono<Void> mono = aopContext.getReturningAs();
        if (aopContext.hasException() || mono.equals(Mono.empty())) {
            update(request, response, aopContext.getExecutionTime());
            finishTrace(request, response, null);

            return;
        }

        final long start = aopContext.getStartNanoTime();
        BiConsumer<Void, Throwable> onSuccessOrError = (t, throwable) -> {
            try {
                update(request, response, System.nanoTime() - start);
            } catch (Exception e) {
                LOG.error("failed to record http incoming metrics", e);
            } finally {
                finishTrace(request, response, throwable);
            }
        };

        // replace the returned Mono so that we can do sth when this request completes
        aopContext.setReturning(mono.doOnSuccess((v) -> onSuccessOrError.accept(null, null))
                                    .doOnError((error) -> onSuccessOrError.accept(null, error)));
    }

    private void update(HttpServerRequest request, HttpServerResponse response, long responseTime) {
        String uri = request.fullPath();

        String srcApplication = request.requestHeaders().get(ITracePropagator.TRACE_HEADER_SRC_APPLICATION);

        int httpStatus = response.status().code();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;

        this.metricRegistry.getOrCreateMetrics(srcApplication, request.method().name(), uri, httpStatus)
                           .updateRequest(responseTime, count4xx, count5xx);
    }

    private void finishTrace(HttpServerRequest request, HttpServerResponse response, Throwable t) {
        if (!(request instanceof IBithonObject)) {
            return;
        }
        Object injected = ((IBithonObject) request).getInjectedObject();
        if (!(injected instanceof HttpServerContext)) {
            return;
        }

        ITraceContext traceContext = ((HttpServerContext) injected).getTraceContext();
        if (traceContext == null) {
            return;
        }

        traceContext.currentSpan()
                    .tag(Tags.HTTP_STATUS, String.valueOf(response.status().code()))
                    .tag(t)
                    .tag((span -> {
                        // extract headers in the response to tag
                        if (!CollectionUtils.isEmpty(responseConfigs.getHeaders())) {
                            HttpHeaders httpHeaders = response.responseHeaders();
                            for (Map.Entry<String, String> entry : responseConfigs.getHeaders().entrySet()) {
                                String headerName = entry.getKey();
                                String tagName = entry.getValue();
                                String tagValue = httpHeaders.get(headerName);
                                if (tagValue != null) {
                                    span.tag(tagName, tagValue);
                                }
                            }
                        }

                        //
                        // handle X-FORWARDED-FOR
                        //
                        if (xforwardTagName == null) {
                            return;
                        }
                        InetSocketAddress remoteAddr = request.remoteAddress();
                        if (remoteAddr == null) {
                            return;
                        }

                        String remoteAddrText = remoteAddr.getAddress().getHostAddress();
                        List<String> xforwarded = request.requestHeaders().getAll(X_FORWARDED_FOR);
                        if (xforwarded == null) {
                            span.tag(xforwardTagName, remoteAddrText);
                            return;
                        }

                        if (!xforwarded.contains(remoteAddrText)) { // prevent duplicates
                            // xforwarded maybe unmodifiable, create a new container
                            xforwarded = new ArrayList<>(xforwarded);

                            xforwarded.add(remoteAddrText);
                        }

                        // using join instead of using ObjectMapper
                        // because in other modules such as tomcat-plugin, we directly get the header value and record it in the log
                        // the value in that header is comma delimited
                        span.tag(xforwardTagName, String.join(",", xforwarded));
                    }))
                    .finish();
        traceContext.finish();
    }
}
