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
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.component.commons.utils.StringUtils;
import sun.net.www.MessageHeader;

/**
 * @author frankchen
 */
public class HttpClient$ParseHTTP extends AbstractInterceptor {

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();

    @Override
    public void onMethodLeave(AopContext aopContext) {
        MessageHeader responseHeader = (MessageHeader) aopContext.getArgs()[0];
        String statusLine = responseHeader.getValue(0);
        Integer statusCode = parseStatusCode(statusLine);

        IBithonObject bithonObject = aopContext.castTargetAs();
        HttpClientContext clientContext = (HttpClientContext) bithonObject.getInjectedObject();
        String httpMethod = clientContext.getMethod();
        String requestUri = clientContext.getUrl();
        if (aopContext.hasException()) {
            metricRegistry.addExceptionRequest(requestUri,
                                               httpMethod,
                                               System.nanoTime() - clientContext.getWriteAt());
        } else {
            metricRegistry.addRequest(requestUri,
                                      httpMethod,
                                      statusCode,
                                      System.nanoTime() - clientContext.getWriteAt())
                          .addByteSize(clientContext.getSentBytes().get(),
                                       clientContext.getReceiveBytes().get());
        }

        ITraceSpan span = TraceContextHolder.currentSpan();
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
    private int parseStatusCode(String statusLine) {
        if (StringUtils.hasText(statusLine)) {
            String[] results = statusLine.split(" ");
            if (results.length >= 1) {
                try {
                    return Integer.parseInt(results[1]);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return -1;
    }
}
