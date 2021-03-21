package com.sbss.bithon.agent.core.metric.http;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.IMetricCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * http client
 *
 * @author frankchen
 */
public class HttpClientMetricCollector implements IMetricCollector {

    private static final int HTTP_CODE_400 = 400;
    private static final int HTTP_CODE_500 = 500;
    private final Map<String, HttpClientMetricSet> metricsMap = new ConcurrentHashMap<>();
    private HttpClientMetricSet metric;

    @Override
    public boolean isEmpty() {
        return metricsMap.isEmpty();
    }

    @Override
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {
        List<Object> messages = new ArrayList<>();
        for (Map.Entry<String, HttpClientMetricSet> entry : metricsMap.entrySet()) {
            metricsMap.compute(entry.getKey(),
                               (k, v) -> getAndRemoveEntry(v));

            messages.add(messageConverter.from(timestamp, interval, this.metric));
        }
        return messages;
    }

    public void addExceptionRequest(String requestUri,
                                    String requestMethod,
                                    long responseTime) {
        String uri = requestUri.split("\\?")[0];
        String requestId = uri.concat("|").concat(requestMethod);

        metricsMap.computeIfAbsent(requestId,
                                   k -> new HttpClientMetricSet(uri,
                                                                requestMethod)).addException(responseTime, 1);
    }

    public void addRequest(String requestUri,
                           String requestMethod,
                           int statusCode,
                           long responseTime) {
        String uri = requestUri.split("\\?")[0];

        int count4xx = 0, count5xx = 0;
        if (statusCode >= HTTP_CODE_400) {
            if (statusCode >= HTTP_CODE_500) {
                count5xx++;
            } else {
                count4xx++;
            }
        }

        String requestId = uri.concat("|").concat(requestMethod);
        metricsMap.computeIfAbsent(requestId,
                                   key -> new HttpClientMetricSet(uri, requestMethod))
                  .add(responseTime, count4xx, count5xx);
    }

    public void addBytes(String requestUri,
                         String requestMethod,
                         long requestBytes,
                         long responseBytes) {
        String uri = requestUri.split("\\?")[0];
        String requestId = uri.concat("|").concat(requestMethod);
        metricsMap.computeIfAbsent(requestId,
                                   k -> new HttpClientMetricSet(uri, requestMethod))
                  .addByteSize(requestBytes, responseBytes);
    }

    private HttpClientMetricSet getAndRemoveEntry(HttpClientMetricSet counter) {
        this.metric = counter;
        return null;
    }
}
