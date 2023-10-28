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

package org.bithon.agent.plugin.httpclient.apache.interceptor;

import org.apache.http.HttpHost;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.protocol.HttpContext;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;
import org.bithon.component.commons.tracing.Tags;

import java.net.InetSocketAddress;

/**
 * {@link org.apache.http.impl.conn.DefaultHttpClientConnectionOperator#connect(ManagedHttpClientConnection, HttpHost, InetSocketAddress, int, SocketConfig, HttpContext)}
 *
 * @author frank.chen021@outlook.com
 * @date 2023/10/25 17:42
 */
public class DefaultHttpClientConnectionOperator$Connect extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceSpanFactory.newSpan("httpclient");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        HttpHost httpHost = aopContext.getArgAs(1);
        aopContext.setUserContext(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                                      .tag(Tags.Http.CLIENT, "apache")
                                      .tag(Tags.Net.PEER, httpHost.getPort() == -1 ? httpHost.getHostName() : httpHost.getHostName() + ":" + httpHost.getPort())
                                      .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getUserContextAs();
        span.tag(aopContext.getException()).finish();
    }
}
