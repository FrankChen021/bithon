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
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.observability.metric.domain.web.HttpIncomingMetricsRegistry;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.propagation.ITracePropagator;
import org.bithon.agent.plugin.jetty.context.RequestContext;
import org.bithon.component.commons.tracing.Tags;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link HttpChannel#onCompleted()}
 *
 * @author frankchen
 */
public class HttpChannel$OnCompleted extends AbstractInterceptor {

    private final HttpIncomingMetricsRegistry metricRegistry = HttpIncomingMetricsRegistry.get();

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        HttpChannel httpChannel = aopContext.getTargetAs();
        Request request = httpChannel.getRequest();

        RequestContext requestContext = (RequestContext) ((IBithonObject) request).getInjectedObject();
        if (requestContext == null) {
            // unknown error
            return InterceptionDecision.SKIP_LEAVE;
        }

        // update metric
        Response response = httpChannel.getResponse();
        update(request, request, response, System.nanoTime() - requestContext.getStartNanoTime());

        //
        // trace
        //
        ITraceContext traceContext = requestContext.getTraceContext();
        if (traceContext != null) {
            ITraceSpan span = traceContext.currentSpan();
            if (span != null) {
                span.tag(Tags.HTTP_STATUS, Integer.toString(httpChannel.getResponse().getStatus())).finish();
            }
            traceContext.finish();
        }

        // clear object reference
        ((IBithonObject) request).setInjectedObject(null);

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        InterceptorContext.remove(InterceptorContext.KEY_URI);
        InterceptorContext.remove(InterceptorContext.KEY_TRACEID);
        TraceContextHolder.remove();
    }

    private void update(Request request, HttpServletRequest httpServletRequest, HttpServletResponse response, long costTime) {
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

        this.metricRegistry.getOrCreateMetrics(srcApplication, httpServletRequest.getMethod(), uri, httpStatus)
                           .updateRequest(costTime, count4xx, count5xx)
                           .updateBytes(requestByteSize, responseByteSize);
    }
}
