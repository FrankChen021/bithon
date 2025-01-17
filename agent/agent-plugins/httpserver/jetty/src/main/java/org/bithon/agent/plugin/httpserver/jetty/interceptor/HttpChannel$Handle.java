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

package org.bithon.agent.plugin.httpserver.jetty.interceptor;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.observability.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.plugin.httpserver.jetty.context.RequestContext;
import org.bithon.component.commons.tracing.Components;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.StringUtils;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.Request;

import java.util.Locale;

/**
 * {@link org.eclipse.jetty.server.HttpChannel#handle()}
 *
 * @author frankchen
 */
public class HttpChannel$Handle extends AroundInterceptor {
    private final HttpIncomingFilter requestFilter = new HttpIncomingFilter();
    private final TraceConfig traceConfig = ConfigurationManager.getInstance().getConfig(TraceConfig.class);

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        TraceContextHolder.detach();
        InterceptorContext.remove(InterceptorContext.KEY_TRACEID);

        HttpChannel httpChannel = aopContext.getTargetAs();

        Request request = httpChannel.getRequest();

        if (httpChannel.getState().getState() == HttpChannelState.State.IDLE) {
            boolean filtered = this.requestFilter.shouldBeExcluded(request.getRequestURI(), request.getHeader("User-Agent"));
            if (filtered) {
                return InterceptionDecision.SKIP_LEAVE;
            }

            ITraceContext traceContext = Tracer.get().propagator().extract(request, Request::getHeader);
            if (traceContext != null) {
                TraceContextHolder.attach(traceContext);
                InterceptorContext.set(InterceptorContext.KEY_TRACEID, traceContext.traceId());

                traceContext.currentSpan()
                            .component(Components.HTTP_SERVER)
                            .tag(Tags.Http.SERVER, "jetty")
                            .tag(Tags.Net.PEER, request.getRemoteAddr() + ":" + request.getRemotePort())
                            .tag(Tags.Http.URL, request.getRequestURI())
                            .tag(Tags.Http.METHOD, request.getMethod())
                            .tag(Tags.Http.VERSION, request.getHttpVersion().toString())
                            .configIfTrue(!traceConfig.getHeaders().getRequest().isEmpty(),
                                          (span) -> traceConfig.getHeaders()
                                                               .getRequest()
                                                               .forEach((header) -> span.tag(Tags.Http.REQUEST_HEADER_PREFIX + header.toLowerCase(Locale.ENGLISH), request.getHeader(header))))
                            .method(aopContext.getTargetClass(), aopContext.getMethod())
                            .kind(SpanKind.SERVER)
                            .start();

                // put the trace id in the header so that the applications have a chance to know whether this request is being sampled
                {
                    request.setAttribute("X-Bithon-TraceId", traceContext.traceId());
                    request.setAttribute("X-Bithon-TraceMode", traceContext.traceMode());

                    String traceIdHeader = traceConfig.getTraceIdResponseHeader();
                    if (StringUtils.hasText(traceIdHeader)) {
                        httpChannel.getResponse().addHeader(traceIdHeader, traceContext.traceId());
                        httpChannel.getResponse().addHeader(traceConfig.getTraceModeResponseHeader(), traceContext.traceMode().text());
                    }
                }
            }

            ((IBithonObject) request).setInjectedObject(new RequestContext(System.nanoTime(), traceContext));
        }

        InterceptorContext.set(InterceptorContext.KEY_URI, request.getRequestURI());

        RequestContext requestContext = (RequestContext) ((IBithonObject) request).getInjectedObject();
        if (requestContext != null) {
            ITraceContext traceContext = requestContext.getTraceContext();
            if (traceContext != null) {
                TraceContextHolder.attach(traceContext);
                InterceptorContext.set(InterceptorContext.KEY_TRACEID, traceContext.traceId());
            }
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        InterceptorContext.remove(InterceptorContext.KEY_URI);
        InterceptorContext.remove(InterceptorContext.KEY_TRACEID);
        TraceContextHolder.detach();
    }
}
