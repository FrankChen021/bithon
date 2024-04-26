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
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetrics;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetricsRegistry;

/**
 * Old http client 4.0.1~4.2.5
 * See {@link org.apache.http.impl.client.DefaultRequestDirector#execute(HttpHost, HttpRequest, HttpContext)}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class DefaultRequestDirector$Execute extends AroundInterceptor {

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        HttpRequest httpRequest = (HttpRequest) aopContext.getArgs()[1];
        String requestUri = httpRequest.getRequestLine().getUri();
        if (InternalHttpClient$DoExecute.shouldExclude(requestUri)) {
            return InterceptionDecision.SKIP_LEAVE;
        } else {
            InterceptorContext.set("apache-http-client.httpRequest", httpRequest);
            return InterceptionDecision.CONTINUE;
        }
    }

    @Override
    public void after(AopContext aopContext) {
        InterceptorContext.remove("apache-http-client.httpRequest");

        HttpRequest httpRequest = (HttpRequest) aopContext.getArgs()[1];
        String requestUri = httpRequest.getRequestLine().getUri();
        String requestMethod = httpRequest.getRequestLine().getMethod();

        HttpOutgoingMetrics metrics;
        if (aopContext.hasException()) {
            metrics = metricRegistry.addExceptionRequest(requestUri, requestMethod, aopContext.getExecutionTime());
        } else {
            HttpResponse httpResponse = aopContext.getReturningAs();
            metrics = metricRegistry.addRequest(requestUri,
                                                requestMethod,
                                                httpResponse.getStatusLine().getStatusCode(),
                                                aopContext.getExecutionTime());
        }

        HttpContext httpContext = aopContext.getArgAs(2);
        if (httpContext == null) {
            return;
        }

        HttpConnection httpConnection = (HttpConnection) httpContext.getAttribute(ExecutionContext.HTTP_CONNECTION);
        if (httpConnection != null) {
            try {
                HttpConnectionMetrics connectionMetrics = httpConnection.getMetrics();
                metrics.addByteSize(connectionMetrics.getSentBytesCount(), connectionMetrics.getReceivedBytesCount());
            } catch (ConnectionShutdownException ignored) {
                /**
                 * This kind of exception has been processed by DefaultRequestDirectorReleaseConnection interceptor
                 * See {@link DefaultRequestDirector$ReleaseConnection}
                 */
            }
        }
    }
}
