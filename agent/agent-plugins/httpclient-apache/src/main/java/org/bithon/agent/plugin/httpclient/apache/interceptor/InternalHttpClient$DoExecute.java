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

import org.apache.http.HttpConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.protocol.HttpContext;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetrics;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetricsRegistry;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Apache http component(client) interceptor for
 * {@link org.apache.http.impl.client.InternalHttpClient#doExecute(HttpHost, HttpRequest, HttpContext)}
 *
 * @author frankchen
 */
public class InternalHttpClient$DoExecute extends AbstractInterceptor {
    /**
     * TODO: changed to configuration
     */
    private static final Set<String> IGNORED_SUFFIXES = new HashSet<>();
    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();

    public static boolean shouldExclude(String uri) {
        if (IGNORED_SUFFIXES.isEmpty()) {
            return false;
        }
        String suffix = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase(Locale.ENGLISH);
        return IGNORED_SUFFIXES.contains(suffix);
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        HttpRequest httpRequest = aopContext.getArgAs(1);
        String requestUri = httpRequest.getRequestLine().getUri();
        return shouldExclude(requestUri) ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        HttpOutgoingMetrics metrics;

        HttpRequest httpRequest = aopContext.getArgAs(1);
        String requestUri = httpRequest.getRequestLine().getUri();
        String requestMethod = httpRequest.getRequestLine().getMethod();

        if (aopContext.hasException()) {
            metrics = metricRegistry.addExceptionRequest(requestUri, requestMethod, aopContext.getExecutionTime());
        } else {
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
                metrics.addByteSize(connectionMetrics.getSentBytesCount(), connectionMetrics.getReceivedBytesCount());
            } catch (ConnectionShutdownException ignored) {
            }
        }
    }
}
