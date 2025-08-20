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

package org.bithon.agent.plugin.httpserver.tomcat.interceptor;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.observability.metric.domain.httpserver.HttpIncomingFilter;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.component.commons.tracing.Components;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;

import java.util.Locale;

/**
 * {@link org.apache.catalina.core.StandardHostValve#invoke(Request, Response)}
 *
 * @author frankchen
 */
public class StandardHostValve$Invoke extends AroundInterceptor {

    private final HttpIncomingFilter requestFilter = new HttpIncomingFilter();
    private final TraceConfig traceConfig = ConfigurationManager.getInstance()
                                                                .getConfig(TraceConfig.class);

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        TraceContextHolder.detach();
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
                    .name(Components.HTTP_SERVER)
                    .tag(Tags.Http.SERVER, "tomcat")
                    .tag(Tags.Net.PEER, request.getRemoteAddr() + ":" + request.getRemotePort())
                    .tag(Tags.Http.URL, request.getRequestURI())
                    .tag(Tags.Http.METHOD, request.getMethod())
                    .tag(Tags.Http.VERSION, request.getProtocol())
                    .configIfTrue(!traceConfig.getHeaders().getRequest().isEmpty(),
                                  (span) -> traceConfig.getHeaders()
                                                       .getRequest()
                                                       .forEach((header) -> span.tag(Tags.Http.REQUEST_HEADER_PREFIX + header.toLowerCase(Locale.ENGLISH), request.getHeader(header))))
                    .method(aopContext.getTargetClass(), aopContext.getMethod())
                    .kind(SpanKind.SERVER)
                    .start();

        aopContext.setUserContext(traceContext);

        // Put the trace id in the header so that the applications have a chance to know whether this request is being sampled
        {
            //
            // Here, we do not use request.getRequest().setAttribute()
            // This is because request.getRequest returns an instance of javax.servlet.HttpServletRequest or jakarta.servlet.HttpServletRequest depending on the tomcat server,
            // However, this plugin is compiled with tomcat 8 which returns javax.servlet.HttpServletRequest
            // On tomcat 10, which requires jakarta.servlet.HttpServletRequest, this request.getRequest() call fails
            //
            request.setAttribute("X-Bithon-TraceId", traceContext.traceId());
            request.setAttribute("X-Bithon-Trace-Mode", traceContext.traceMode().text());

            String traceIdHeader = traceConfig.getTraceIdResponseHeader();
            if (StringUtils.hasText(traceIdHeader)) {
                request.getResponse().addHeader(traceIdHeader, traceContext.traceId());
                request.getResponse().addHeader(traceConfig.getTraceModeResponseHeader(), traceContext.traceMode().text());
            }
        }

        TraceContextHolder.attach(traceContext);
        InterceptorContext.set(InterceptorContext.KEY_URI, request.getRequestURI());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        InterceptorContext.remove(InterceptorContext.KEY_URI);
        TraceContextHolder.detach();

        ITraceContext traceContext = aopContext.getUserContext();
        try {
            Response response = (Response) aopContext.getArgs()[1];
            traceContext.currentSpan()
                        .tag(Tags.Http.STATUS, Integer.toString(response.getStatus()))
                        .tag(aopContext.getException())
                        .configIfTrue(CollectionUtils.isNotEmpty(traceConfig.getHeaders().getResponse()),
                                      (s) -> {
                                          for (String header : traceConfig.getHeaders().getResponse()) {
                                              String value = response.getHeader(header);
                                              if (value != null) {
                                                  s.tag(Tags.Http.RESPONSE_HEADER_PREFIX + header.toLowerCase(Locale.ENGLISH), value);
                                              }
                                          }
                                      })
                        .finish();
        } finally {
            traceContext.finish();
        }
    }
}
