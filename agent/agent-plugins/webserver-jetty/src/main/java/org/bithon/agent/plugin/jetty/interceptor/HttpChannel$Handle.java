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
import org.bithon.agent.core.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.core.tracing.Tracer;
import org.bithon.agent.core.tracing.config.TraceConfig;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.plugin.jetty.context.RequestContext;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.Request;

/**
 * {@link org.eclipse.jetty.server.HttpChannel#handle()}
 *
 * @author frankchen
 */
public class HttpChannel$Handle extends AbstractInterceptor {
    private HttpIncomingFilter requestFilter;
    private TraceConfig traceConfig;

    @Override
    public boolean initialize() {
        requestFilter = new HttpIncomingFilter();

        traceConfig = AgentContext.getInstance().getAgentConfiguration().getConfig(TraceConfig.class);

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        HttpChannel httpChannel = aopContext.castTargetAs();

        Request request = httpChannel.getRequest();

        if (httpChannel.getState().getState() == HttpChannelState.State.IDLE) {
            boolean filtered = this.requestFilter.shouldBeExcluded(request.getRequestURI(), request.getHeader("User-Agent"));
            if (filtered) {
                return InterceptionDecision.SKIP_LEAVE;
            }

            ITraceContext traceContext = Tracer.get().propagator().extract(request, (carrier, key) -> carrier.getHeader(key));
            if (traceContext != null) {
                TraceContextHolder.set(traceContext);
                InterceptorContext.set(InterceptorContext.KEY_TRACEID, traceContext.traceId());

                traceContext.currentSpan()
                            .component("jetty")
                            .tag("uri", request.getRequestURI())
                            .tag("method", request.getMethod())
                            .tag((span) -> traceConfig.getHeaders().forEach((header) -> span.tag("header." + header, request.getHeader(header))))
                            .method(aopContext.getMethod())
                            .kind(SpanKind.SERVER)
                            .start();
            }

            httpChannel.getRequest().setAttribute("bithon.context", new RequestContext(System.nanoTime(), traceContext));
        }

        RequestContext requestContext = (RequestContext) (httpChannel.getRequest().getAttribute("bithon.context"));

        InterceptorContext.set(InterceptorContext.KEY_URI, request.getRequestURI());
        if (requestContext != null) {
            TraceContextHolder.set(requestContext.getTraceContext());
            InterceptorContext.set(InterceptorContext.KEY_TRACEID, requestContext.getTraceContext().traceId());
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        InterceptorContext.remove(InterceptorContext.KEY_URI);
        InterceptorContext.remove(InterceptorContext.KEY_TRACEID);
        TraceContextHolder.remove();
    }
}
