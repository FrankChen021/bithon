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
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.observability.metric.domain.httpserver.HttpIncomingMetricsRegistry;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.propagation.ITracePropagator;
import org.bithon.agent.plugin.httpserver.jetty12.context.RequestContext;
import org.bithon.component.commons.tracing.Tags;
import org.eclipse.jetty.server.HttpStream;

import java.util.Locale;

/**
 * {@link org.eclipse.jetty.server.internal.HttpChannelState.HandlerInvoker#completeStream(HttpStream, Throwable)}
 *
 * @author frank.chen021@outlook.com
 * @date 16/3/25 11:52 pm
 */
public class HandlerInvoker$CompleteStream extends BeforeInterceptor {
    private final TraceConfig traceConfig = ConfigurationManager.getInstance().getConfig(TraceConfig.class);
    private final HttpIncomingMetricsRegistry metricRegistry = HttpIncomingMetricsRegistry.get();

    @Override
    public void before(AopContext aopContext) {
        IBithonObject bithonObject = aopContext.getTargetAs();
        RequestContext requestContext = (RequestContext) bithonObject.getInjectedObject();
        if (requestContext == null) {
            return;
        }

        ITraceSpan span = null;
        if (requestContext.getTraceContext() != null) {
            span = requestContext.getTraceContext().currentSpan();
            span.tag(Tags.Http.STATUS, Integer.toString(requestContext.getChannelResponse().getStatus()))
                .tag((Throwable) aopContext.getArgAs(1))
                .configIfTrue(!traceConfig.getHeaders().getResponse().isEmpty(),
                              (s) -> traceConfig.getHeaders()
                                                .getResponse()
                                                .forEach((header) -> {
                                                    String value = requestContext.getChannelResponse().getHeaders().get(header);
                                                    if (value != null) {
                                                        s.tag(Tags.Http.RESPONSE_HEADER_PREFIX + header.toLowerCase(Locale.ENGLISH), value);
                                                    }
                                                }))
                .tag(Tags.Http.REQUEST_HEADER_PREFIX + "content-length", requestContext.getChannelRequest().getContentBytesRead())
                .tag(Tags.Http.RESPONSE_HEADER_PREFIX + "content-length", requestContext.getChannelResponse().getContentBytesWritten())
                .finish();
            span.context().finish();
        }

        String srcApplication = requestContext.getChannelRequest().getHeaders().get(ITracePropagator.TRACE_HEADER_SRC_APPLICATION);
        String uri = requestContext.getChannelRequest().getHttpURI().getPath();
        int httpStatus = requestContext.getChannelResponse().getStatus();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;
        long duration = span != null ? (span.endTime() - span.startTime()) * 1000 : System.nanoTime() - requestContext.getChannelRequest().getBeginNanoTime();
        this.metricRegistry.getOrCreateMetrics(srcApplication, requestContext.getChannelRequest().getMethod(), uri, httpStatus)
                           .updateRequest(duration, count4xx, count5xx)
                           .updateBytes(requestContext.getChannelRequest().getContentBytesRead(), requestContext.getChannelResponse().getContentBytesWritten());
    }
}
