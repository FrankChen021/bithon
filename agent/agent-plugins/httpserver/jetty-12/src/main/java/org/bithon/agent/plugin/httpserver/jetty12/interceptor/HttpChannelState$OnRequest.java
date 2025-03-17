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
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.metric.domain.httpserver.HttpIncomingFilter;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.plugin.httpserver.jetty12.context.RequestContext;
import org.bithon.component.commons.tracing.Components;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.internal.HttpChannelState;

import java.util.Locale;

/**
 * {@link org.eclipse.jetty.server.internal.HttpChannelState#onRequest(MetaData.Request)}
 *
 * @author frank.chen021@outlook.com
 * @date 16/3/25 11:46 pm
 */
public class HttpChannelState$OnRequest extends AfterInterceptor {
    private final HttpIncomingFilter requestFilter = new HttpIncomingFilter();
    private final TraceConfig traceConfig = ConfigurationManager.getInstance().getConfig(TraceConfig.class);

    @Override
    public void after(AopContext aopContext) {
        Runnable handlerInvoker = aopContext.getReturningAs();
        if (handlerInvoker == null) {
            return;
        }

        if (!(handlerInvoker instanceof IBithonObject)) {
            // Should be HandlerInvoker
            return;
        }

        HttpChannelState channelState = aopContext.getTargetAs();
        Request request = channelState.getRequest();
        boolean filtered = this.requestFilter.shouldBeExcluded(request.getHttpURI().getPath(),
                                                               request.getHeaders().get("User-Agent"));
        if (filtered) {
            return;
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
                        .kind(SpanKind.SERVER)
                        .start();
        }
        ((IBithonObject) handlerInvoker).setInjectedObject(new RequestContext((HttpChannelState.ChannelRequest) channelState.getRequest(),
                                                                              (HttpChannelState.ChannelResponse) channelState.getResponse(),
                                                                              traceContext));
    }
}
