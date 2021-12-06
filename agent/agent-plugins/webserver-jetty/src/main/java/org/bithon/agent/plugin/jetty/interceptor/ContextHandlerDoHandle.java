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

package org.bithon.agent.plugin.jetty.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.core.metric.domain.web.HttpIncomingMetricsCollector;
import org.bithon.agent.core.tracing.Tracer;
import org.bithon.agent.core.tracing.config.TraceConfig;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.core.tracing.propagation.ITracePropagator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author frankchen
 */
public class ContextHandlerDoHandle extends AbstractInterceptor {
    private HttpIncomingFilter requestFilter;

    private HttpIncomingMetricsCollector metricsCollector;
    private TraceConfig traceConfig;


    @Override
    public boolean initialize() {
        requestFilter = new HttpIncomingFilter();

        metricsCollector = MetricCollectorManager.getInstance()
                                                 .getOrRegister("jetty-web-request-metrics",
                                                                HttpIncomingMetricsCollector.class);

        traceConfig = AgentContext.getInstance()
                                  .getAgentConfiguration()
                                  .getConfig(TraceConfig.class);

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        Request request = (Request) aopContext.getArgs()[1];
        boolean filtered = this.requestFilter.shouldBeExcluded(request.getRequestURI(), request.getHeader("User-Agent"));
        if (filtered) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        InterceptorContext.set(InterceptorContext.KEY_URI, request.getRequestURI());

        ITraceContext traceContext = Tracer.get()
                                           .propagator()
                                           .extract(request, (carrier, key) -> carrier.getHeader(key));
        if (traceContext != null) {
            TraceContextHolder.set(traceContext);
            InterceptorContext.set(InterceptorContext.KEY_TRACEID, traceContext.traceId());

            traceContext.currentSpan()
                        .component("jetty")
                        .tag("uri", request.getRequestURI())
                        .tag((span) -> traceConfig.getHeaders().forEach((header) -> span.tag("header." + header, request.getHeader(header))))
                        .method(aopContext.getMethod())
                        .kind(SpanKind.SERVER)
                        .start();
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        // update metric
        update((Request) aopContext.getArgs()[1],
               (HttpServletRequest) aopContext.getArgs()[2],
               (HttpServletResponse) aopContext.getArgs()[3],
               aopContext.getCostTime());

        //
        // trace
        //
        InterceptorContext.remove(InterceptorContext.KEY_URI);
        InterceptorContext.remove(InterceptorContext.KEY_TRACEID);

        ITraceContext traceContext = null;
        ITraceSpan span = null;
        try {
            traceContext = TraceContextHolder.current();
            if (traceContext == null) {
                return;
            }
            span = traceContext.currentSpan();
            if (span == null) {
                // TODO: ERROR
                return;
            }

            HttpServletResponse response = (HttpServletResponse) aopContext.getArgs()[3];
            span.tag("status", Integer.toString(response.getStatus()));
            if (aopContext.hasException()) {
                span.tag("exception", aopContext.getException().toString());
            }
        } finally {
            if (span != null) {
                span.finish();
            }
            if (traceContext != null) {
                traceContext.finish();
            }
            try {
                TraceContextHolder.remove();
            } catch (Exception ignored) {
            }
        }
    }

    private void update(Request request,
                        HttpServletRequest httpServletRequest,
                        HttpServletResponse response,
                        long costTime) {
        String srcApplication = request.getHeader(ITracePropagator.TRACE_HEADER_SRC_APPLICATION);
        String uri = httpServletRequest.getRequestURI();
        int httpStatus = response.getStatus();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;
        long requestByteSize = request.getContentRead();
        long responseByteSize = 0;
        if (response instanceof Response) {
            Response jettyResponse = (Response) response;
            responseByteSize = jettyResponse.getContentCount();
        }

        this.metricsCollector.getOrCreateMetrics(srcApplication,
                                                 uri,
                                                 httpStatus)
                             .updateRequest(costTime, count4xx, count5xx)
                             .updateBytes(requestByteSize, responseByteSize);
    }
}
