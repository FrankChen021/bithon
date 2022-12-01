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

import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpRequest;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.impl.client.DefaultRequestDirector;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.metric.domain.http.HttpOutgoingMetricsRegistry;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.lang.reflect.Field;

/**
 * {@link DefaultRequestDirector#releaseConnection()}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class DefaultRequestDirector$ReleaseConnection extends AbstractInterceptor {
    private static final ILogAdaptor log = LoggerFactory.getLogger(DefaultRequestDirector$ReleaseConnection.class);

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();
    private Field managedConnectionField;

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
            metricRegistry.addBytes(requestUri, requestMethod, requestBytes, responseBytes);
        } catch (ConnectionShutdownException e) {
            log.warn("Failed to get metric on HTTP Connection since it has been shutdown", e);
        }

        return InterceptionDecision.SKIP_LEAVE;
    }
}
