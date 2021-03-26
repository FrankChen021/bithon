package com.sbss.bithon.agent.core.metric.domain.http;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IntervalMetricCollector;

import java.util.List;

/**
 * http client
 *
 * @author frankchen
 */
public class HttpClientMetricCollector extends IntervalMetricCollector<HttpClientCompositeMetric> {

    private static final int HTTP_CODE_400 = 400;
    private static final int HTTP_CODE_500 = 500;

    @Override
    protected HttpClientCompositeMetric newMetrics() {
        return new HttpClientCompositeMetric();
    }

    @Override
    protected Object toMessage(IMessageConverter messageConverter,
                               int interval,
                               long timestamp,
                               List<String> dimensions,
                               HttpClientCompositeMetric metric) {
        return messageConverter.from(timestamp, interval, dimensions, metric);
    }

    public void addExceptionRequest(String requestUri,
                                    String requestMethod,
                                    long responseTime) {
        String uri = requestUri.split("\\?")[0];
        getOrCreateMetric(uri, requestMethod).addException(responseTime, 1);
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

        getOrCreateMetric(uri, requestMethod).add(responseTime, count4xx, count5xx);
    }

    public void addBytes(String requestUri,
                         String requestMethod,
                         long requestBytes,
                         long responseBytes) {
        String uri = requestUri.split("\\?")[0];
        getOrCreateMetric(uri, requestMethod).addByteSize(requestBytes, responseBytes);
    }
}
