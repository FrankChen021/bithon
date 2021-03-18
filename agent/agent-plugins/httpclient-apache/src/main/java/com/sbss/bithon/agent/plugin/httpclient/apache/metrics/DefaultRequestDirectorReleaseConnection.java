package com.sbss.bithon.agent.plugin.httpclient.apache.metrics;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.http.HttpClientMetricCollector;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpRequest;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.impl.client.DefaultRequestDirector;
import org.apache.http.impl.conn.ConnectionShutdownException;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class DefaultRequestDirectorReleaseConnection extends AbstractInterceptor {
    private static Logger log = LoggerFactory.getLogger(DefaultRequestDirectorReleaseConnection.class);

    private HttpClientMetricCollector metricProvider;
    private Field managedConnectionField;

    @Override
    public boolean initialize() throws Exception {
        metricProvider = MetricCollectorManager.getInstance()
                                               .getOrRegister("apache-http-client", HttpClientMetricCollector.class);

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {

        HttpRequest httpRequest = InterceptorContext.getAs("apache-http-client.httpRequest");
        if (httpRequest == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        /*
         * NOTE:
         *   The code snippet below can't be put into initialize() method above or a 'duplicate class' error would be thrown
         *
         *   This is because this interceptor is installed on 'DefaultRequestDirector' class,
         * and 'initialize' method is called before instrumentation and the code below would cause loading of 'DefaultRequestDirector' before instrumentation
         * See: https://github.com/raphw/byte-buddy/issues/757
         */
        if (managedConnectionField == null) {
            // no need to worry about concurrent problem here, it's acceptable if two threads are executing the code block below
            // But for concurrency, what needs to be paid attention is that a temporary variable is assigned first to hold the result
            Field field = DefaultRequestDirector.class.getDeclaredField("managedConn");
            field.setAccessible(true);
            managedConnectionField = field;
        }

        ManagedHttpClientConnection connection = (ManagedHttpClientConnection) managedConnectionField.get(aopContext.getTarget());
        if (connection == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        try {
            HttpConnectionMetrics connectionMetrics = connection.getMetrics();
            long requestBytes = connectionMetrics.getSentBytesCount();
            long responseBytes = connectionMetrics.getReceivedBytesCount();

            String requestUri = httpRequest.getRequestLine().getUri();
            String requestMethod = httpRequest.getRequestLine().getMethod();
            metricProvider.addBytes(requestUri, requestMethod, requestBytes, responseBytes);
        } catch (ConnectionShutdownException e) {
            log.warn("Failed to get metric on HTTP Connection since it has been shutdown", e);
        }

        return InterceptionDecision.SKIP_LEAVE;
    }
}
