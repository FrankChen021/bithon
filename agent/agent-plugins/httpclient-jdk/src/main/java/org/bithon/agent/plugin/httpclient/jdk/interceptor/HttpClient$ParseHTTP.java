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

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.AfterInterceptor;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetricsRegistry;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.StringUtils;
import sun.net.ProgressSource;
import sun.net.www.MessageHeader;
import sun.net.www.protocol.http.HttpURLConnection;

import java.util.List;
import java.util.Map;

/**
 * {@link sun.net.www.http.HttpClient#parseHTTP(MessageHeader, ProgressSource, HttpURLConnection)}
 *
 * @author frankchen
 */
public class HttpClient$ParseHTTP extends AfterInterceptor {

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();

    private final TraceConfig traceConfig = ConfigurationManager.getInstance().getConfig(TraceConfig.class);

    /**
     * {@link HttpClientContext} accessed in this method is injected in {@link HttpClient$New} or {@link HttpsClient$New}
     */
    @Override
    public void after(AopContext aopContext) {
        MessageHeader responseHeader = (MessageHeader) aopContext.getArgs()[0];
        String statusLine = responseHeader.getValue(0);
        Integer statusCode = parseStatusCode(statusLine);

        IBithonObject bithonObject = aopContext.getTargetAs();
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

        // Record configured response headers in tracing logs
        if (!traceConfig.getHeaders().getResponse().isEmpty()) {
            Map<String, List<String>> headers = responseHeader.getHeaders();
            traceConfig.getHeaders()
                       .getResponse()
                       .forEach((name) -> {
                           List<String> values = headers.get(name);
                           if (values != null && !values.isEmpty()) {
                               span.tag("http.response.header." + name, values.get(0));
                           }
                       });
        }

        span.tag(Tags.HTTP_STATUS, statusCode.toString())
            .finish();
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
