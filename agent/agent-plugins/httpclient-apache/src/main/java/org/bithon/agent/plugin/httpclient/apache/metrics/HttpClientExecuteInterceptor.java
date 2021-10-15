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
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.impl.execchain.MinimalClientExec;
import org.apache.http.impl.execchain.RedirectExec;
import org.apache.http.protocol.HttpContext;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.http.HttpOutgoingMetricsCollector;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Apache http component(client) interceptor
 *
 * @author frankchen
 */
public class HttpClientExecuteInterceptor extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(HttpClientExecuteInterceptor.class);
    private static final Set<String> IGNORED_SUFFIXES = new HashSet<>();
    private HttpOutgoingMetricsCollector metricCollector;
    private boolean isNewVersion = true;

    public static boolean filter(String uri) {
        String suffix = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase();
        return IGNORED_SUFFIXES.contains(suffix);
    }

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("apache-http-client", HttpOutgoingMetricsCollector.class);

        try {
            Class.forName("org.apache.http.impl.execchain.MinimalClientExec");
        } catch (ClassNotFoundException e) {
            isNewVersion = false;
        }
        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        Object targetObject = aopContext.getTarget();
        Object[] args = aopContext.getArgs();

        if (isNewVersion && targetObject instanceof MinimalClientExec) {
            //
            // "http client 4.3.4~4.5.3: MinimalClientExec"
            //
            HttpRequestWrapper httpRequest = (HttpRequestWrapper) args[1];
            String requestUri = httpRequest.getRequestLine().getUri();
            return filter(requestUri) ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;

        } else if (isNewVersion && targetObject instanceof RedirectExec) {
            //
            // http client 4.3.4~4.5.3: RedirectExec"
            //
            HttpRequestWrapper httpRequestWrapper = (HttpRequestWrapper) args[1];
            String requestUri = httpRequestWrapper.getOriginal().getRequestLine().getUri();
            return filter(requestUri) ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;

        } else if (isNewVersion) {
            //
            // "http client 4.3.4~4.5.3: InternalHttpClient"
            //
            HttpRequest httpRequest = (HttpRequest) args[1];
            String requestUri = httpRequest.getRequestLine().getUri();
            return filter(requestUri) ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;
        } else {
            return InterceptionDecision.SKIP_LEAVE;
        }
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Object targetObject = aopContext.getTarget();
        Object[] args = aopContext.getArgs();
        boolean hasException = aopContext.getException() != null;
        long costTime = aopContext.getCostTime();

        if (isNewVersion && targetObject instanceof MinimalClientExec) {
            //
            // "http client 4.3.4~4.5.3: MinimalClientExec"
            //
            HttpRequestWrapper httpRequest = (HttpRequestWrapper) args[1];
            String requestUri = httpRequest.getRequestLine().getUri();
            String requestMethod = httpRequest.getRequestLine().getMethod();

            if (hasException) {
                metricCollector.addExceptionRequest(requestUri, requestMethod, costTime);
            } else {
                HttpResponse httpResponse = aopContext.castReturningAs();
                metricCollector.addRequest(requestUri,
                                           requestMethod,
                                           httpResponse.getStatusLine().getStatusCode(),
                                           costTime);

                HttpContext httpContext = (HttpContext) args[2];
                if (httpContext != null && httpContext.getAttribute("http.connection") != null) {
                    HttpConnection httpConnection = (HttpConnection) httpContext.getAttribute("http.connection");

                    HttpConnectionMetrics connectionMetrics = httpConnection.getMetrics();
                    long requestBytes = connectionMetrics.getSentBytesCount();
                    long responseBytes = connectionMetrics.getReceivedBytesCount();
                    metricCollector.addBytes(requestUri, requestMethod, requestBytes, responseBytes);
                }
            }
        } else if (isNewVersion && targetObject instanceof RedirectExec) {
            //
            // http client 4.3.4~4.5.3: RedirectExec"
            //

            HttpRequestWrapper httpRequestWrapper = (HttpRequestWrapper) args[1];
            if (hasException) {
                String requestUri = httpRequestWrapper.getRequestLine().getUri();
                String requestMethod = httpRequestWrapper.getRequestLine().getMethod();
                metricCollector.addExceptionRequest(requestUri, requestMethod, costTime);
            } else {

                HttpContext httpContext = (HttpContext) args[2];

                String requestUri = httpRequestWrapper.getOriginal().getRequestLine().getUri();
                String requestMethod = httpRequestWrapper.getOriginal().getRequestLine().getMethod();
                if (httpContext != null && httpContext.getAttribute("http.connection") != null) {
                    HttpConnection httpConnection = (HttpConnection) httpContext.getAttribute("http.connection");

                    HttpConnectionMetrics connectionMetrics = httpConnection.getMetrics();
                    long requestBytes = connectionMetrics.getSentBytesCount();
                    long responseBytes = connectionMetrics.getReceivedBytesCount();
                    metricCollector.addBytes(requestUri, requestMethod, requestBytes, responseBytes);
                }
            }
        } else if (isNewVersion) {
            //
            // "http client 4.3.4~4.5.3: InternalHttpClient"
            //
            HttpRequest httpRequest = (HttpRequest) args[1];
            String requestUri = httpRequest.getRequestLine().getUri();
            String requestMethod = httpRequest.getRequestLine().getMethod();

            if (hasException) {
                metricCollector.addExceptionRequest(requestUri, requestMethod, costTime);
            } else {
                metricCollector.addRequest(requestUri,
                                           requestMethod,
                                           ((HttpResponse) aopContext.getReturning()).getStatusLine().getStatusCode(),
                                           costTime);
            }
        } else {
            log.warn("http client version not supported!");
        }
    }
}
