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

package org.bithon.agent.plugin.httpserver.undertow.interceptor;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.observability.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.observability.metric.domain.web.HttpIncomingMetricsRegistry;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.propagation.ITracePropagator;
import org.bithon.component.commons.tracing.Components;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.StringUtils;

import java.util.Locale;

/**
 * @author frankchen
 */
public class HttpServerExchangeDispatch extends BeforeInterceptor {

    private final HttpIncomingFilter requestFilter = new HttpIncomingFilter();
    private final HttpIncomingMetricsRegistry metricRegistry = HttpIncomingMetricsRegistry.get();
    private final TraceConfig traceConfig = ConfigurationManager.getInstance().getConfig(TraceConfig.class);

    @Override
    public void before(AopContext aopContext) {
        final HttpServerExchange exchange = aopContext.getTargetAs();

        if (this.requestFilter.shouldBeExcluded(exchange.getRequestPath(), exchange.getRequestHeaders().getFirst("User-Agent"))) {
            return;
        }

        final ITraceContext traceContext = Tracer.get()
                                                 .propagator()
                                                 .extract(exchange.getRequestHeaders(), HeaderMap::getFirst);

        if (traceContext != null) {
            traceContext.currentSpan()
                        .component(Components.HTTP_SERVER)
                        .tag(Tags.Http.SERVER, "undertow")
                        .tag(Tags.Net.PEER, exchange.getConnection().getPeerAddress())
                        .tag(Tags.Http.URL, exchange.getRequestURI())
                        .tag(Tags.Http.METHOD, exchange.getRequestMethod().toString())
                        .tag(Tags.Http.VERSION, exchange.getProtocol().toString())
                        .configIfTrue(!traceConfig.getHeaders().getRequest().isEmpty(),
                                      (span) -> traceConfig.getHeaders()
                                                           .getRequest()
                                                           .forEach((header) -> span.tag(Tags.Http.REQUEST_HEADER_PREFIX + header.toLowerCase(Locale.ENGLISH),
                                                                                         exchange.getRequestHeaders().getFirst(header))))
                        .method(aopContext.getTargetClass(), aopContext.getMethod())
                        .kind(SpanKind.SERVER)
                        .start();

            {
                ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                servletRequestContext.getServletRequest().setAttribute("X-Bithon-TraceId", traceContext.traceId());
                servletRequestContext.getServletRequest().setAttribute("X-Trace-Mode", traceContext.traceMode().text());

                String traceIdHeader = traceConfig.getTraceIdResponseHeader();
                if (StringUtils.hasText(traceIdHeader)) {
                    exchange.getResponseHeaders().add(HttpString.tryFromString(traceIdHeader), traceContext.traceId());
                    exchange.getResponseHeaders().add(HttpString.tryFromString(traceConfig.getTraceModeResponseHeader()), traceContext.traceMode().text());
                }
            }
        }
        final long startTime = System.nanoTime();
        exchange.addExchangeCompleteListener((httpExchange, nextListener) -> {
            try {
                update(httpExchange, startTime);

                if (traceContext != null) {
                    traceContext.currentSpan().tag(Tags.Http.STATUS, Integer.toString(httpExchange.getStatusCode())).finish();
                    traceContext.finish();
                }
            } catch (Exception ignored) {
            }

            nextListener.proceed();
        });
    }

    private void update(HttpServerExchange exchange, long startNano) {
        String srcApplication = exchange.getRequestHeaders().getLast(ITracePropagator.TRACE_HEADER_SRC_APPLICATION);
        String method = exchange.getRequestMethod().toString();
        String uri = exchange.getRequestPath();
        int httpStatus = exchange.getStatusCode();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;
        long requestByteSize = exchange.getRequestContentLength() < 0 ? 0 : exchange.getRequestContentLength();
        long responseByteSize = exchange.getResponseBytesSent();
        long costTime = System.nanoTime() - startNano;

        this.metricRegistry.getOrCreateMetrics(srcApplication, method, uri, httpStatus)
                           .updateRequest(costTime, count4xx, count5xx)
                           .updateBytes(requestByteSize, responseByteSize);
    }
}
