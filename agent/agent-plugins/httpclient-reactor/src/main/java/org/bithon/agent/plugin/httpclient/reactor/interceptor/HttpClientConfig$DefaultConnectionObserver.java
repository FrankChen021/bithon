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

package org.bithon.agent.plugin.httpclient.reactor.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.netty.http.client.HttpClientConfig;

/**
 * {@link reactor.netty.http.client.HttpClientConfig#defaultConnectionObserver()}
 *
 * @author frank.chen021@outlook.com
 * @date 9/9/24 3:29 pm
 */
public class HttpClientConfig$DefaultConnectionObserver extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        // This object is attached in the HttpClientFinalizer$Connect
        ITraceSpan span = aopContext.getInjectedOnTargetAs();
        if (span == null) {
            return;
        }

        // Hook and change the connection observer
        HttpClientConfig clientConfig = aopContext.getTargetAs();
        ConnectionObserver current = aopContext.getReturningAs();
        aopContext.setReturning(current.then(new ConnectionStateObserver(clientConfig, span)));
    }

    private static class ConnectionStateObserver implements ConnectionObserver {
        private ITraceSpan span;
        private final String connectionProvider;

        public ConnectionStateObserver(HttpClientConfig clientConfig, ITraceSpan span) {
            this.span = span;
            this.connectionProvider = clientConfig.connectionProvider().getClass().getName();
        }

        @Override
        public void onUncaughtException(Connection connection, Throwable error) {
            span.tag(error).finish();
            span.context().finish();
        }

        @Override
        public void onStateChange(Connection connection, State newState) {
            if (newState.equals(State.CONNECTED) && span != null) {
                span.tag(Tags.Net.PEER, connection.channel().remoteAddress())
                    .tag(Tags.Http.CLIENT, "reactor")
                    // Since there is a network connection to remote, we mark it as a client span
                    .kind(SpanKind.CLIENT)
                    .finish();
                span.context().finish();
                span = null;
            }
            if (newState.equals(State.ACQUIRED) && span != null) {
                // A connection is retrieved from the pool,
                // set the span name to 'acquire' and do not mark it as SpanKind.CLIENT
                span.method(connectionProvider, "acquire")
                    .tag(Tags.Net.PEER, connection.channel().remoteAddress())
                    .finish();
                span.context().finish();
                span = null;
            }
        }
    }
}
