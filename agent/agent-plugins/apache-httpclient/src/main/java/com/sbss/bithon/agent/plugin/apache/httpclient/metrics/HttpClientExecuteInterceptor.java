package com.sbss.bithon.agent.plugin.apache.httpclient.metrics;

import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.metrics.http.HttpClientMetricProvider;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import org.apache.http.HttpConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.impl.execchain.MinimalClientExec;
import org.apache.http.impl.execchain.RedirectExec;
import org.apache.http.protocol.HttpContext;
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

    private HttpClientMetricProvider metricProvider;
    private final static Set<String> ignoredSuffixes = new HashSet<>();
    private boolean isNewVersion = true;

    @Override
    public boolean initialize() {
        metricProvider = MetricProviderManager.getInstance().register("apache-http-client", new HttpClientMetricProvider());

        try {
            Class.forName("org.apache.http.impl.execchain.MinimalClientExec");
        } catch (ClassNotFoundException e) {
            isNewVersion = false;
        }
        return true;
    }

    public static boolean filter(String uri) {
        String suffix = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase();
        return ignoredSuffixes.contains(suffix);
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
                metricProvider.addExceptionRequest(requestUri, requestMethod, costTime);
            } else {
                HttpResponse httpResponse = aopContext.castReturningAs();
                metricProvider.addRequest(requestUri, requestMethod, httpResponse.getStatusLine().getStatusCode(), costTime);

                HttpContext httpContext = (HttpContext) args[2];
                if (httpContext != null && httpContext.getAttribute("http.connection") != null) {
                    HttpConnection httpConnection = (HttpConnection) httpContext.getAttribute("http.connection");

                    HttpConnectionMetrics connectionMetrics = httpConnection.getMetrics();
                    long requestBytes = connectionMetrics.getSentBytesCount();
                    long responseBytes = connectionMetrics.getReceivedBytesCount();
                    metricProvider.addBytes(requestUri, requestMethod, requestBytes, responseBytes);
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
                metricProvider.addExceptionRequest(requestUri, requestMethod, costTime);
            } else {

                HttpContext httpContext = (HttpContext) args[2];

                String requestUri = httpRequestWrapper.getOriginal().getRequestLine().getUri();
                String requestMethod = httpRequestWrapper.getOriginal().getRequestLine().getMethod();
                if (httpContext != null && httpContext.getAttribute("http.connection") != null) {
                    HttpConnection httpConnection = (HttpConnection) httpContext.getAttribute("http.connection");

                    HttpConnectionMetrics connectionMetrics = httpConnection.getMetrics();
                    long requestBytes = connectionMetrics.getSentBytesCount();
                    long responseBytes = connectionMetrics.getReceivedBytesCount();
                    metricProvider.addBytes(requestUri, requestMethod, requestBytes, responseBytes);
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
                metricProvider.addExceptionRequest(requestUri, requestMethod, costTime);
            } else {
                metricProvider.addRequest(requestUri,
                        requestMethod,
                        ((HttpResponse) aopContext.getReturning()).getStatusLine().getStatusCode(),
                        costTime);
            }
        } else {
            log.warn("http client version not supported!");
        }
    }
}
