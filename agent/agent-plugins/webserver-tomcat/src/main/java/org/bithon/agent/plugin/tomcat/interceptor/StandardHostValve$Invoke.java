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

package org.bithon.agent.plugin.tomcat.interceptor;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.config.ConfigurationManager;
import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.core.tracing.Tracer;
import org.bithon.agent.core.tracing.config.TraceConfig;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.core.tracing.propagation.TraceMode;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.StringUtils;

/**
 * {@link org.apache.catalina.core.StandardHostValve#invoke(Request, Response)}
 *
 * @author frankchen
 */
public class StandardHostValve$Invoke extends AbstractInterceptor {

    private HttpIncomingFilter requestFilter;
    private TraceConfig traceConfig;

    @Override
    public boolean initialize() {
        requestFilter = new HttpIncomingFilter();
        traceConfig = ConfigurationManager.getInstance()
                                          .getConfig(TraceConfig.class);

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        TraceContextHolder.remove();
        InterceptorContext.remove(InterceptorContext.KEY_TRACEID);

        Request request = (Request) aopContext.getArgs()[0];
        String uri = request.getRequestURI();
        if (uri == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        if (requestFilter.shouldBeExcluded(request.getRequestURI(), request.getHeader("User-Agent"))) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ITraceContext traceContext = Tracer.get()
                                           .propagator()
                                           .extract(request, Request::getHeader);
        if (traceContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        traceContext.currentSpan()
                    .component("tomcat")
                    .tag(Tags.REMOTE_ADDR, request.getRemoteAddr())
                    .tag(Tags.HTTP_URI, request.getRequestURI())
                    .tag(Tags.HTTP_METHOD, request.getMethod())
                    .tag(Tags.HTTP_VERSION, request.getProtocol())
                    .tag((span) -> traceConfig.getHeaders()
                                              .getRequest()
                                              .forEach((header) -> span.tag("http.header." + header, request.getHeader(header))))
                    .method(aopContext.getMethod())
                    .kind(SpanKind.SERVER)
                    .start();

        TraceContextHolder.set(traceContext);

        // Put the trace id in the header so that the applications have chance to know whether this request is being sampled
        if (traceContext.traceMode().equals(TraceMode.TRACE)) {
            request.getRequest().setAttribute("X-Bithon-TraceId", traceContext.traceId());

            String traceIdHeader = traceConfig.getTraceIdInResponse();
            if (StringUtils.hasText(traceIdHeader)) {
                request.getResponse().addHeader(traceIdHeader, traceContext.traceId());
            }
        }

        aopContext.setUserContext(traceContext);

        InterceptorContext.set(InterceptorContext.KEY_URI, request.getRequestURI());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        InterceptorContext.remove(InterceptorContext.KEY_URI);

        ITraceContext traceContext = aopContext.getUserContextAs();
        try {
            Response response = (Response) aopContext.getArgs()[1];
            traceContext.currentSpan()
                        .tag(Tags.HTTP_STATUS, Integer.toString(response.getStatus()))
                        .tag(aopContext.getException())
                        .finish();
        } finally {
            traceContext.finish();
            try {
                TraceContextHolder.remove();
            } catch (Exception ignored) {
            }
        }
    }
}
