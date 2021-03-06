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

package org.bithon.agent.plugin.httpclient.apache.metrics;

import org.apache.http.HttpConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.metric.domain.http.HttpOutgoingMetricsRegistry;

/**
 * Old http client 4.0.1~4.2.5
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class DefaultRequestDirector$Execute extends AbstractInterceptor {

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        HttpRequest httpRequest = (HttpRequest) aopContext.getArgs()[1];
        String requestUri = httpRequest.getRequestLine().getUri();
        if (HttpClientExecuteInterceptor.filter(requestUri)) {
            return InterceptionDecision.SKIP_LEAVE;
        } else {
            InterceptorContext.set("apache-http-client.httpRequest", httpRequest);
            return InterceptionDecision.CONTINUE;
        }
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        InterceptorContext.remove("apache-http-client.httpRequest");

        HttpRequest httpRequest = (HttpRequest) aopContext.getArgs()[1];
        String requestUri = httpRequest.getRequestLine().getUri();
        String requestMethod = httpRequest.getRequestLine().getMethod();

        if (aopContext.hasException()) {
            metricRegistry.addExceptionRequest(requestUri, requestMethod, aopContext.getCostTime());
            return;
        }

        HttpResponse httpResponse = aopContext.castReturningAs();
        metricRegistry.addRequest(requestUri,
                                  requestMethod,
                                  httpResponse.getStatusLine().getStatusCode(),
                                  aopContext.getCostTime());

        HttpContext httpContext = (HttpContext) aopContext.getArgs()[2];
        if (httpContext == null) {
            return;
        }

        HttpConnection httpConnection = (HttpConnection) httpContext.getAttribute(ExecutionContext.HTTP_CONNECTION);
        if (httpConnection == null) {
            return;
        }

        try {
            HttpConnectionMetrics connectionMetrics = httpConnection.getMetrics();
            long requestBytes = connectionMetrics.getSentBytesCount();
            long responseBytes = connectionMetrics.getReceivedBytesCount();
            metricRegistry.addBytes(requestUri, requestMethod, requestBytes, responseBytes);
        } catch (ConnectionShutdownException e) {
            // This kind of exception has been processed by DefaultRequestDirectorReleaseConnection interceptor
        }
    }
}
