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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.core.metric.domain.web.HttpIncomingMetricsCollector;
import org.bithon.agent.core.tracing.Tracer;
import org.bithon.agent.core.tracing.config.TraceConfig;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.core.tracing.propagation.ITracePropagator;
import org.bithon.agent.plugin.spring.webflux.context.HttpServerContext;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

/**
 * {@link org.springframework.http.server.reactive.ReactorHttpHandlerAdapter#apply}
 *
 * @author Frank Chen
 * @date 7/10/21 3:16 pm
 */
public class ReactorHttpHandlerAdapter$Apply extends AbstractInterceptor {

    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ReactorHttpHandlerAdapter$Apply.class);

    private HttpIncomingMetricsCollector metricCollector;
    private HttpIncomingFilter requestFilter;
    private TraceConfig traceConfig;

    @Override
    public boolean initialize() {
        requestFilter = new HttpIncomingFilter();

        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("webflux-request-metrics",
                                                               HttpIncomingMetricsCollector.class);

        traceConfig = AgentContext.getInstance()
                                  .getAgentConfiguration()
                                  .getConfig(TraceConfig.class);

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
                                                         .extract(request,
                                                                  (carrier, key) -> request.requestHeaders().get(key));

                traceContext.currentSpan()
                            .component("webflux")
                            .tag("uri", request.fullPath())
                            .tag("method", request.method().name())
                            .tag((span) -> traceConfig.getHeaders().forEach((header) -> span.tag("header." + header, request.requestHeaders().get(header))))
                            .method(aopContext.getMethod())
                            .kind(SpanKind.SERVER)
                            .start();

                ((HttpServerContext) injected).setTraceContext(traceContext);
            }
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        final HttpServerRequest request = (HttpServerRequest) aopContext.getArgs()[0];
        final HttpServerResponse response = (HttpServerResponse) aopContext.getArgs()[1];

        Mono<Void> mono = aopContext.castReturningAs();
        if (aopContext.hasException() || mono.equals(Mono.empty())) {
            update(request, response, aopContext.getCostTime());
            finishTrace(request, response);

            return;
        }

        final long start = System.nanoTime();
        // replace the returned Mono so that we can do sth when this request completes
        aopContext.setReturning(mono.doOnSuccessOrError((success, error) -> {
            try {
                update(request, response, System.nanoTime() - start);
            } catch (Exception e) {
                LOG.error("failed to record http incoming metrics", e);
            } finally {
                finishTrace(request, response);
            }
        }));

        //DO NOT remove the thread-local variable of trace context
    }

    private void update(HttpServerRequest request, HttpServerResponse response, long responseTime) {
        String uri = request.fullPath();

        String srcApplication = request.requestHeaders().get(ITracePropagator.TRACE_HEADER_SRC_APPLICATION);

        int httpStatus = response.status().code();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;

        this.metricCollector.getOrCreateMetrics(srcApplication, uri, httpStatus)
                            .updateRequest(responseTime, count4xx, count5xx);
    }

    private void finishTrace(HttpServerRequest request, HttpServerResponse response) {
        if (!(request instanceof IBithonObject)) {
            return;
        }
        Object injected = ((IBithonObject) request).getInjectedObject();
        if (!(injected instanceof HttpServerContext)) {
            return;
        }

        ITraceContext traceContext = ((HttpServerContext) injected).getTraceContext();
        if (traceContext != null) {
            traceContext.currentSpan().tag("status", String.valueOf(response.status().code())).finish();
            traceContext.finish();
        }
    }
}
