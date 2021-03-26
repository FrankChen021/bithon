package com.sbss.bithon.agent.plugin.undertow.metric;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IntervalMetricCollector;
import com.sbss.bithon.agent.core.metric.domain.web.WebRequestCompositeMetric;
import io.undertow.server.HttpServerExchange;

import java.util.List;

/**
 * @author frankchen
 */
public class WebRequestMetricCollector extends IntervalMetricCollector<WebRequestCompositeMetric> {

    public void update(HttpServerExchange exchange, long startNano) {
        String srcApplication = exchange.getRequestHeaders().getLast(InterceptorContext.HEADER_SRC_APPLICATION_NAME);
        String uri = exchange.getRequestPath();
        int errorCount = exchange.getStatusCode() >= 400 ? 1 : 0;
        int httpStatus = exchange.getStatusCode();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 && httpStatus < 600 ? 1 : 0;
        long requestByteSize = exchange.getRequestContentLength() < 0 ? 0 : exchange.getRequestContentLength();
        long responseByteSize = exchange.getResponseBytesSent();
        long costTime = System.nanoTime() - startNano;

        WebRequestCompositeMetric counter = getOrCreateMetric(srcApplication == null ? "" : srcApplication, uri);
        counter.updateRequest(costTime, errorCount, count4xx, count5xx);
        counter.updateBytes(requestByteSize, responseByteSize);
    }

    @Override
    protected WebRequestCompositeMetric newMetrics() {
        return new WebRequestCompositeMetric();
    }

    @Override
    protected Object toMessage(IMessageConverter messageConverter,
                               int interval,
                               long timestamp,
                               List<String> dimensions,
                               WebRequestCompositeMetric metric) {
        return messageConverter.from(timestamp, interval, dimensions, metric);
    }
}
