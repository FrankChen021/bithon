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

package com.sbss.bithon.agent.plugin.jetty.interceptor;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.web.RequestUriFilter;
import com.sbss.bithon.agent.core.metric.domain.web.UserAgentFilter;
import com.sbss.bithon.agent.core.tracing.Tracer;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.context.TraceContextHolder;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import com.sbss.bithon.agent.plugin.jetty.metric.WebRequestMetricCollector;
import org.eclipse.jetty.server.Request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author frankchen
 */
public class ContextHandlerDoHandle extends AbstractInterceptor {
    private RequestUriFilter uriFilter;
    private UserAgentFilter userAgentFilter;

    private WebRequestMetricCollector requestMetricCollector;

    @Override
    public boolean initialize() {
        uriFilter = new RequestUriFilter();
        userAgentFilter = new UserAgentFilter();

        requestMetricCollector = MetricCollectorManager.getInstance()
                                                       .getOrRegister("jetty-web-request-metrics",
                                                                      WebRequestMetricCollector.class);

        return true;
    }


    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        Request request = (Request) aopContext.getArgs()[1];
        boolean filtered = this.userAgentFilter.isFiltered(request.getHeader("User-Agent"))
                           || this.uriFilter.isFiltered(request.getRequestURI());
        if (filtered) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        InterceptorContext.set(InterceptorContext.KEY_URI, request.getRequestURI());

        TraceContext traceContext = Tracer.get()
                                          .propagator()
                                          .extract(request, (carrier, key) -> carrier.getHeader(key));
        if (traceContext != null) {
            TraceContextHolder.set(traceContext);
            InterceptorContext.set(InterceptorContext.KEY_TRACEID, traceContext.traceId());

            traceContext.currentSpan()
                        .component("jetty")
                        .tag("uri", request.getRequestURI())
                        .clazz(aopContext.getTargetClass())
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

        TraceContext traceContext = null;
        TraceSpan span = null;
        try {
            traceContext = TraceContextHolder.get();
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
