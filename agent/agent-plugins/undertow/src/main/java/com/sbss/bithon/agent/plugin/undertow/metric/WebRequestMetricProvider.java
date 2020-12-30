package com.sbss.bithon.agent.plugin.undertow.metric;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.metrics.IMetricProvider;
import com.sbss.bithon.agent.core.metrics.web.WebRequestMetric;
import io.undertow.server.HttpServerExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 */
public class WebRequestMetricProvider implements IMetricProvider {

    private WebRequestMetric counter;
    private final Map<String, WebRequestMetric> counters = new ConcurrentHashMap<>();

    private static final WebRequestMetricProvider INSTANCE = new WebRequestMetricProvider();

    public static WebRequestMetricProvider getInstance() {
        return INSTANCE;
    }

    WebRequestMetricProvider() {
        MetricProviderManager.getInstance().register("undertow-webRequest", this);
    }

    public void update(HttpServerExchange exchange, long startNanoTime) {
        String uri = exchange.getRequestPath();
        int errorCount = exchange.getStatusCode() >= 400 ? 1 : 0;
        int httpStatus = exchange.getStatusCode();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 && httpStatus < 600 ? 1 : 0;
        long requestByteSize = exchange.getRequestContentLength() < 0 ? 0 : exchange.getRequestContentLength();
        long responseByteSize = exchange.getResponseBytesSent();
        long costTime = System.nanoTime() - startNanoTime;

        WebRequestMetric counter = counters.computeIfAbsent(uri, WebRequestMetric::new);
        counter.add(costTime, errorCount, count4xx, count5xx);
        counter.addByteSize(requestByteSize, responseByteSize);
    }

    @Override
    public boolean isEmpty() {
        return counters.isEmpty();
    }

    @Override
    public List<Object> buildMessages(IMessageConverter messageConverter,
                                      AppInstance appInstance,
                                      int interval,
                                      long timestamp) {
        List<Object> messages = new ArrayList<>();
        for (Map.Entry<String, WebRequestMetric> entry : counters.entrySet()) {
            counters.compute(entry.getKey(),
                             (k,
                              v) -> tempAndRemoveEntry(v));
            messages.add(messageConverter.from(appInstance,
                                               timestamp,
                                               interval,
                                               this.counter));
        }
        return messages;
    }

    private WebRequestMetric tempAndRemoveEntry(WebRequestMetric counter) {
        this.counter = counter;
        return null;
    }
}
