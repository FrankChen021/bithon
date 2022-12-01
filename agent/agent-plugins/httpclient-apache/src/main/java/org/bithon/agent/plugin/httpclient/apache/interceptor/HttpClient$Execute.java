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
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.impl.execchain.MinimalClientExec;
import org.apache.http.impl.execchain.RedirectExec;
import org.apache.http.protocol.HttpContext;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.metric.domain.http.HttpOutgoingMetrics;
import org.bithon.agent.core.metric.domain.http.HttpOutgoingMetricsRegistry;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Apache http component(client) interceptor
 *
 * @author frankchen
 */
public class HttpClient$Execute extends AbstractInterceptor {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(HttpClient$Execute.class);
    private static final Set<String> IGNORED_SUFFIXES = new HashSet<>();
    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();
    private boolean isNewVersion = true;

    public static boolean shouldExclude(String uri) {
        if (IGNORED_SUFFIXES.isEmpty()) {
            return false;
        }
        String suffix = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase(Locale.ENGLISH);
        return IGNORED_SUFFIXES.contains(suffix);
    }

    @Override
    public boolean initialize() {
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
            return shouldExclude(requestUri) ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;

        } else if (isNewVersion && targetObject instanceof RedirectExec) {
            //
            // http client 4.3.4~4.5.3: RedirectExec"
            //
            HttpRequestWrapper httpRequestWrapper = (HttpRequestWrapper) args[1];
            String requestUri = httpRequestWrapper.getOriginal().getRequestLine().getUri();
            return shouldExclude(requestUri) ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;

        } else if (isNewVersion) {
            //
            // "http client 4.3.4~4.5.3: InternalHttpClient"
            //
            HttpRequest httpRequest = (HttpRequest) args[1];
            String requestUri = httpRequest.getRequestLine().getUri();
            return shouldExclude(requestUri) ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;
        } else {
            return InterceptionDecision.SKIP_LEAVE;
        }
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Object targetObject = aopContext.getTarget();
        Object[] args = aopContext.getArgs();
        long costTime = aopContext.getCostTime();

        HttpOutgoingMetrics metrics;
        if (isNewVersion && targetObject instanceof MinimalClientExec) {
            //
            // "http client 4.3.4~4.5.3: MinimalClientExec"
            //
            HttpRequestWrapper httpRequest = (HttpRequestWrapper) args[1];
            String requestUri = httpRequest.getRequestLine().getUri();
            String requestMethod = httpRequest.getRequestLine().getMethod();

            if (aopContext.hasException()) {
                metrics = metricRegistry.addExceptionRequest(requestUri, requestMethod, costTime);
            } else {
                HttpResponse httpResponse = aopContext.castReturningAs();
                metrics = metricRegistry.addRequest(requestUri,
                                                    requestMethod,
                                                    httpResponse.getStatusLine().getStatusCode(),
                                                    costTime);
            }
        } else if (isNewVersion && targetObject instanceof RedirectExec) {
            //
            // http client 4.3.4~4.5.3: RedirectExec"
            //
            HttpRequestWrapper httpRequestWrapper = (HttpRequestWrapper) args[1];
            if (aopContext.hasException()) {
                String requestUri = httpRequestWrapper.getRequestLine().getUri();
                String requestMethod = httpRequestWrapper.getRequestLine().getMethod();
                metrics = metricRegistry.addExceptionRequest(requestUri, requestMethod, costTime);
            } else {
                String requestUri = httpRequestWrapper.getOriginal().getRequestLine().getUri();
                String requestMethod = httpRequestWrapper.getOriginal().getRequestLine().getMethod();

                metrics = metricRegistry.addRequest(requestUri,
                                                    requestMethod,
                                                    ((HttpResponse) aopContext.getReturning()).getStatusLine().getStatusCode(),
                                                    costTime);
            }
        } else if (isNewVersion) {
            //
            // "http client 4.3.4~4.5.3: InternalHttpClient"
            //
            HttpRequest httpRequest = (HttpRequest) args[1];
            String requestUri = httpRequest.getRequestLine().getUri();
            String requestMethod = httpRequest.getRequestLine().getMethod();

            if (aopContext.hasException()) {
                metrics = metricRegistry.addExceptionRequest(requestUri, requestMethod, costTime);
            } else {
                metrics = metricRegistry.addRequest(requestUri,
                                                    requestMethod,
                                                    ((HttpResponse) aopContext.getReturning()).getStatusLine().getStatusCode(),
                                                    costTime);
            }
        } else {
            LOG.warn("http client version not supported!");
            return;
        }

        HttpContext httpContext = (HttpContext) args[2];
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
