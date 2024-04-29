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

package org.bithon.agent.plugin.apache.httpcomponents4.interceptor;

import org.apache.http.HttpConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.protocol.HttpContext;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetrics;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetricsRegistry;

/**
 * Apache http component(client) interceptor
 *
 * @author frankchen
 */
public class RedirectExec$Execute extends AroundInterceptor {
    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        HttpRequestWrapper httpRequestWrapper = aopContext.getArgAs(1);
        String requestUri = httpRequestWrapper.getOriginal().getRequestLine().getUri();
        return InternalHttpClient$DoExecute.shouldExclude(requestUri) ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        HttpOutgoingMetrics metrics;

        HttpRequestWrapper httpRequestWrapper = aopContext.getArgAs(1);
        if (aopContext.hasException()) {
            String requestUri = httpRequestWrapper.getRequestLine().getUri();
            String requestMethod = httpRequestWrapper.getRequestLine().getMethod();
            metrics = metricRegistry.addExceptionRequest(requestUri, requestMethod, aopContext.getExecutionTime());
        } else {
            String requestUri = httpRequestWrapper.getOriginal().getRequestLine().getUri();
            String requestMethod = httpRequestWrapper.getOriginal().getRequestLine().getMethod();

            metrics = metricRegistry.addRequest(requestUri,
                                                requestMethod,
                                                ((HttpResponse) aopContext.getReturning()).getStatusLine().getStatusCode(),
                                                aopContext.getExecutionTime());
        }

        HttpContext httpContext = aopContext.getArgAs(2);
        HttpConnection httpConnection = (HttpConnection) (httpContext == null ? null : httpContext.getAttribute("http.connection"));
        if (httpConnection != null) {
            try {
                HttpConnectionMetrics connectionMetrics = httpConnection.getMetrics();
                metrics.updateIOMetrics(connectionMetrics.getSentBytesCount(), connectionMetrics.getReceivedBytesCount());
            } catch (ConnectionShutdownException ignored) {
            }
        }
    }
}
