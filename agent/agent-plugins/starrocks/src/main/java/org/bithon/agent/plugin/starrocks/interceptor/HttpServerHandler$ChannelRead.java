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

package org.bithon.agent.plugin.starrocks.interceptor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.TraceMode;
import org.bithon.component.commons.tracing.Components;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

import java.net.InetSocketAddress;
import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/5/15 18:35
 */
public class HttpServerHandler$ChannelRead extends BeforeInterceptor
{

    private final TraceConfig traceConfig = ConfigurationManager.getInstance()
                                                                .getConfig(TraceConfig.class);

    @Override
    public void before(AopContext aopContext) throws Exception {
        
        ChannelHandlerContext ctx = (ChannelHandlerContext) aopContext.getArgs()[0];
        HttpRequest request = (HttpRequest) aopContext.getArgs()[1];

        ITraceContext traceContext = Tracer.get()
                                           .propagator()
                                           .extract(request, (req, key) -> req.headers().get(key));
        if (traceContext == null) {
            return;
        }

        InetSocketAddress socket = (InetSocketAddress) ctx.channel().remoteAddress();

        traceContext.currentSpan()
                    .component(Components.HTTP_SERVER)
                    .tag(Tags.Http.SERVER, "sr_fe")
                    .tag(Tags.Net.PEER_ADDR, socket.getHostName() + ":" + socket.getPort())
                    .tag(Tags.Http.URL, request.uri())
                    .tag(Tags.Http.METHOD, request.getMethod())
                    .tag(Tags.Http.VERSION, request.protocolVersion())
                    .configIfTrue(!traceConfig.getHeaders().getRequest().isEmpty(),
                                  (span) -> traceConfig.getHeaders()
                                                       .getRequest()
                                                       .forEach((header) -> span.tag(Tags.Http.REQUEST_HEADER_PREFIX + header.toLowerCase(Locale.ENGLISH), request.headers().get(header))))
                    .method(aopContext.getTargetClass(), aopContext.getMethod())
                    .kind(SpanKind.SERVER)
                    .start();

        TraceContextHolder.set(traceContext);

        // Put the trace id in the header so that the applications have a chance to know whether this request is being sampled
        if (traceContext.traceMode().equals(TraceMode.TRACING)) {
            //
            // Here, we do not use request.getRequest().setAttribute()
            // This is because request.getRequest returns an instance of javax.servlet.HttpServletRequest or jakarta.servlet.HttpServletRequest depending on the tomcat server,
            // However, this plugin is compiled with tomcat 8 which returns javax.servlet.HttpServletRequest
            // On tomcat 10, which requires jakarta.servlet.HttpServletRequest, this request.getRequest() call fails
            //
            request.headers().set("X-Bithon-TraceId", traceContext.traceId());
        }

        aopContext.setUserContext(traceContext);

        InterceptorContext.set(InterceptorContext.KEY_URI, request.uri());
    }
}
