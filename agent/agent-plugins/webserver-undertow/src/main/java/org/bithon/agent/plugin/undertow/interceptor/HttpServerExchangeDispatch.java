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

package org.bithon.agent.plugin.undertow.interceptor;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.HeaderMap;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.core.metric.domain.web.HttpIncomingMetricsRegistry;
import org.bithon.agent.core.tracing.Tracer;
import org.bithon.agent.core.tracing.config.TraceConfig;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.Tags;
import org.bithon.agent.core.tracing.propagation.ITracePropagator;
import org.bithon.agent.core.tracing.propagation.TraceMode;

/**
 * @author frankchen
 */
public class HttpServerExchangeDispatch extends AbstractInterceptor {

    private final HttpIncomingFilter requestFilter = new HttpIncomingFilter();
    private final HttpIncomingMetricsRegistry metricRegistry = HttpIncomingMetricsRegistry.get();
    private final TraceConfig traceConfig = AgentContext.getInstance()
                                                        .getAgentConfiguration()
                                                        .getConfig(TraceConfig.class);


    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        final HttpServerExchange exchange = aopContext.castTargetAs();

        if (this.requestFilter.shouldBeExcluded(exchange.getRequestPath(), exchange.getRequestHeaders().getFirst("User-Agent"))) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        final ITraceContext traceContext = Tracer.get()
                                                 .propagator()
                                                 .extract(exchange.getRequestHeaders(), HeaderMap::getFirst);

        final ITraceSpan rootSpan = traceContext.currentSpan()
                                                .component("undertow")
                                                .tag(Tags.HTTP_URI, exchange.getRequestURI())
                                                .tag(Tags.HTTP_METHOD, exchange.getRequestMethod().toString())
                                                .tag(Tags.HTTP_VERSION, exchange.getProtocol().toString())
                                                .tag((span) -> traceConfig.getHeaders()
                                                                          .forEach((header) -> span.tag("http.header." + header,
                                                                                                        exchange.getRequestHeaders().getFirst(header))))
                                                .method(aopContext.getMethod())
                                                .kind(SpanKind.SERVER)
                                                .start();

        if (traceContext.traceMode().equals(TraceMode.TRACE)) {
            ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
            servletRequestContext.getServletRequest().setAttribute("X-Bithon-TraceId", traceContext.traceId());
        }

        final long startTime = System.nanoTime();
        exchange.addExchangeCompleteListener((httpExchange, nextListener) -> {

            try {
                update(httpExchange, startTime);

                rootSpan.tag(Tags.HTTP_STATUS, Integer.toString(httpExchange.getStatusCode())).finish();
                traceContext.finish();
            } catch (Exception ignored) {
            }

            nextListener.proceed();
        });
        return InterceptionDecision.SKIP_LEAVE;
    }

    private void update(HttpServerExchange exchange, long startNano) {
        String srcApplication = exchange.getRequestHeaders().getLast(ITracePropagator.TRACE_HEADER_SRC_APPLICATION);
        String uri = exchange.getRequestPath();
        int httpStatus = exchange.getStatusCode();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;
        long requestByteSize = exchange.getRequestContentLength() < 0 ? 0 : exchange.getRequestContentLength();
        long responseByteSize = exchange.getResponseBytesSent();
        long costTime = System.nanoTime() - startNano;

        this.metricRegistry.getOrCreateMetrics(srcApplication, uri, httpStatus)
                           .updateRequest(costTime, count4xx, count5xx)
                           .updateBytes(requestByteSize, responseByteSize);
    }
}
