package com.sbss.bithon.agent.plugin.httpclient.apache.metrics;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.http.HttpClientMetricCollector;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import org.apache.http.HttpConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

/**
 * Old http client 4.0.1~4.2.5
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class DefaultRequestDirectorExecute extends AbstractInterceptor {

    private HttpClientMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("apache-http-client", HttpClientMetricCollector.class);
        return true;
    }

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
            metricCollector.addExceptionRequest(requestUri, requestMethod, aopContext.getCostTime());
            return;
        }

        HttpResponse httpResponse = aopContext.castReturningAs();
        metricCollector.addRequest(requestUri,
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
            metricCollector.addBytes(requestUri, requestMethod, requestBytes, responseBytes);
        } catch (ConnectionShutdownException e) {
            // This kind of exception has been processed by DefaultRequestDirectorReleaseConnection interceptor
        }
    }
}
