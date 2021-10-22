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
import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.core.tracing.Tracer;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.plugin.jetty.metric.WebRequestMetricCollector;
import org.eclipse.jetty.server.Request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author frankchen
 */
public class ContextHandlerDoHandle extends AbstractInterceptor {
    private HttpIncomingFilter requestFilter;

    private WebRequestMetricCollector requestMetricCollector;

    @Override
    public boolean initialize() {
        requestFilter = new HttpIncomingFilter();

        requestMetricCollector = MetricCollectorManager.getInstance()
                                                       .getOrRegister("jetty-web-request-metrics",
                                                                      WebRequestMetricCollector.class);

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
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
                        .method(aopContext.getMethod())
                        .kind(SpanKind.SERVER)
                        .start();
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        // update metric
        requestMetricCollector.update((Request) aopContext.getArgs()[1],
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
            try {
                if (span != null) {
                    span.finish();
                }
            } catch (Exception ignored) {
            }
            try {
                if (traceContext != null) {
                    traceContext.finish();
                }
            } catch (Exception ignored) {
            }
            try {
                TraceContextHolder.remove();
            } catch (Exception ignored) {
            }
        }
    }
}
