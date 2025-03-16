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

package org.bithon.agent.plugin.httpserver.jetty12.interceptor;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.metric.domain.httpserver.HttpIncomingFilter;
import org.bithon.agent.observability.metric.domain.httpserver.HttpIncomingMetricsRegistry;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.propagation.ITracePropagator;
import org.bithon.component.commons.tracing.Components;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.StringUtils;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.HttpStream;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.function.Function;

/**
 * {@link org.eclipse.jetty.server.Server#handle(Request, Response, Callback)}
 *
 * @author frankchen
 */
public class Server$Handle extends AroundInterceptor {
    private final HttpIncomingMetricsRegistry metricRegistry = HttpIncomingMetricsRegistry.get();

    private final HttpIncomingFilter requestFilter = new HttpIncomingFilter();
    private final TraceConfig traceConfig = ConfigurationManager.getInstance().getConfig(TraceConfig.class);

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        Request request = aopContext.getArgAs(0);
        Response response = aopContext.getArgAs(1);

        boolean filtered = this.requestFilter.shouldBeExcluded(request.getHttpURI().getPath(),
                                                               request.getHeaders().get("User-Agent"));
        if (filtered) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ITraceContext traceContext = Tracer.get().propagator().extract(request, (carrier, key) -> carrier.getHeaders().get(key));
        if (traceContext != null) {
            traceContext.currentSpan()
                        .component(Components.HTTP_SERVER)
                        .tag(Tags.Http.SERVER, "jetty")
                        .tag(Tags.Net.PEER, request.getConnectionMetaData().getRemoteSocketAddress())
                        .tag(Tags.Http.URL, request.getHttpURI() == null ? null : request.getHttpURI().getPathQuery())
                        .tag(Tags.Http.METHOD, request.getMethod())
                        .tag(Tags.Http.VERSION, request.getConnectionMetaData().getHttpVersion())
                        .configIfTrue(!traceConfig.getHeaders().getRequest().isEmpty(),
                                      (span) -> traceConfig.getHeaders()
                                                           .getRequest()
                                                           .forEach((header) -> span.tag(Tags.Http.REQUEST_HEADER_PREFIX + header.toLowerCase(Locale.ENGLISH), request.getHeaders().get(header))))
                        .method(aopContext.getTargetClass(), aopContext.getMethod())
                        .kind(SpanKind.SERVER)
                        .start();

            // put the trace id in the header so that the applications have a chance to know whether this request is being sampled
            {
                request.setAttribute("X-Bithon-TraceId", traceContext.traceId());
                request.setAttribute("X-Bithon-TraceMode", traceContext.traceMode());

                String traceIdHeader = traceConfig.getTraceIdResponseHeader();
                if (StringUtils.hasText(traceIdHeader)) {
                    response.getHeaders().add(traceIdHeader, traceContext.traceId());
                    response.getHeaders().add(traceConfig.getTraceModeResponseHeader(), traceContext.traceMode().text());
                }
            }
        }

        final ITraceSpan span = traceContext == null ? null : traceContext.currentSpan();
        request.addHttpStreamWrapper(new Function<HttpStream, HttpStream>() {
            @Override
            public HttpStream apply(HttpStream httpStream) {
                return new HttpStream.Wrapper(httpStream) {
                    @Override
                    public Content.Chunk read() {
                        return super.read();
                    }

                    @Override
                    public void send(MetaData.Request request, MetaData.Response response, boolean last, ByteBuffer content, Callback callback) {
                        super.send(request, response, last, content, callback);
                    }

                    @Override
                    public void succeeded() {
                        onRequestEnd(aopContext.getStartNanoTime(), span, request, response, null);
                        super.succeeded();
                    }

                    @Override
                    public void failed(Throwable x) {
                        onRequestEnd(aopContext.getStartNanoTime(), span, request, response, x);
                        super.failed(x);
                    }
                };
            }
        });

        return InterceptionDecision.CONTINUE;
    }

    private void onRequestEnd(long start, ITraceSpan span, Request request, Response response, Throwable t) {
        if (span != null) {
            span.tag(Tags.Http.STATUS, Integer.toString(response.getStatus()))
                .tag(t)
                .configIfTrue(!traceConfig.getHeaders().getResponse().isEmpty(),
                              (s) -> traceConfig.getHeaders()
                                                .getResponse()
                                                .forEach((header) -> {
                                                    String value = response.getHeaders().get(header);
                                                    if (value != null) {
                                                        s.tag(Tags.Http.RESPONSE_HEADER_PREFIX + header.toLowerCase(Locale.ENGLISH), value);
                                                    }
                                                }))
                .finish();
            span.context().finish();
        }

        String srcApplication = request.getHeaders().get(ITracePropagator.TRACE_HEADER_SRC_APPLICATION);
        String uri = request.getHttpURI().getPath();
        int httpStatus = response.getStatus();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;

        this.metricRegistry.getOrCreateMetrics(srcApplication, request.getMethod(), uri, httpStatus)
                           .updateRequest(System.nanoTime() - start, count4xx, count5xx)
                           .updateBytes(0, 0);
    }
}
