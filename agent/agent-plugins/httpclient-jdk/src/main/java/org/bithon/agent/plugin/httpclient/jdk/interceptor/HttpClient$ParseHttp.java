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

package org.bithon.agent.plugin.httpclient.jdk.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.core.metric.domain.http.HttpOutgoingMetricsRegistry;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.Tags;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.component.commons.utils.StringUtils;
import sun.net.www.MessageHeader;
import sun.net.www.protocol.http.HttpURLConnection;

/**
 * @author frankchen
 */
public class HttpClient$ParseHttp extends AbstractInterceptor {

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();

    @Override
    public void onMethodLeave(AopContext aopContext) {
        MessageHeader responseHeader = (MessageHeader) aopContext.getArgs()[0];
        String statusLine = responseHeader.getValue(0);
        Integer statusCode = parseStatusCode(statusLine);

        IBithonObject bithonObject = aopContext.castTargetAs();
        HttpURLConnection connection = (HttpURLConnection) bithonObject.getInjectedObject();
        String httpMethod = connection.getRequestMethod();
        String requestUri = connection.getURL().toString();
        if (aopContext.hasException()) {
            // TODO: aopContext.getCostTime here only returns the execution time of HttpClient.parseHTTP
            metricRegistry.addExceptionRequest(requestUri, httpMethod, aopContext.getCostTime());
        } else {
            metricRegistry.addRequest(requestUri, httpMethod, statusCode, aopContext.getCostTime());
        }

        ITraceContext traceContext = TraceContextHolder.current();
        if (traceContext == null) {
            return;
        }
        ITraceSpan span = traceContext.currentSpan();
        if (span == null) {
            return;
        }

        span.tag(Tags.HTTP_STATUS, statusCode.toString()).finish();
    }

    /**
     * eg:
     * HTTP/1.0 200 OK
     * HTTP/1.0 401 Unauthorized
     * It will return 200 and 401 respectively. Returns -1 if no code can be discerned
     */
    private Integer parseStatusCode(String statusLine) {
        if (StringUtils.hasText(statusLine)) {
            String[] results = statusLine.split(" ");
            if (results.length >= 1) {
                try {
                    return Integer.valueOf(results[1]);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return -1;
    }
}
