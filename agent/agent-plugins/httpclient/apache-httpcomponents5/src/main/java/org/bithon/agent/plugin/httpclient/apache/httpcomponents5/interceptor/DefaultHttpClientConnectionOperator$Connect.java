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

package org.bithon.agent.plugin.httpclient.apache.httpcomponents5.interceptor;

import org.apache.hc.client5.http.io.ManagedHttpClientConnection;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Timeout;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.tracing.Tags;

import java.net.InetSocketAddress;

/**
 * {@link org.apache.hc.client5.http.impl.io.DefaultHttpClientConnectionOperator#connect(ManagedHttpClientConnection, HttpHost, InetSocketAddress, Timeout, SocketConfig, Object, HttpContext)}
 *
 * @author frank.chen021@outlook.com
 * @date 2023/10/25 17:42
 */
public class DefaultHttpClientConnectionOperator$Connect extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceContextFactory.newSpan("http-client");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        HttpHost httpHost = aopContext.getArgAs(1);
        aopContext.setSpan(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                               .tag(Tags.Http.CLIENT, "apache-httpcomponents-5")
                               .tag(Tags.Net.PEER, httpHost.getPort() == -1 ? httpHost.getHostName() : httpHost.getHostName() + ":" + httpHost.getPort())
                               .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();
        span.tag(aopContext.getException()).finish();
    }
}
