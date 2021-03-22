package com.sbss.bithon.agent.plugin.undertow.metric;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.IMetricCollector;
import com.sbss.bithon.agent.core.metric.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.web.WebRequestMetricSet;
import io.undertow.server.HttpServerExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 */
public class WebRequestMetricCollector implements IMetricCollector {

    private WebRequestMetricSet metric;
    private final Map<String, WebRequestMetricSet> metricsMap = new ConcurrentHashMap<>();

    private static final WebRequestMetricCollector INSTANCE = new WebRequestMetricCollector();

    public static WebRequestMetricCollector getInstance() {
        return INSTANCE;
    }

    WebRequestMetricCollector() {
        MetricCollectorManager.getInstance().register("undertow-webRequest", this);
    }

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

        WebRequestMetricSet counter = metricsMap.computeIfAbsent(srcApplication + "|" + uri,
                                                              key -> new WebRequestMetricSet(srcApplication, uri));
        counter.updateRequest(costTime, errorCount, count4xx, count5xx);
        counter.updateBytes(requestByteSize, responseByteSize);
    }

    @Override
    public boolean isEmpty() {
        return metricsMap.isEmpty();
    }

    @Override
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {
        List<Object> messages = new ArrayList<>();
        for (Map.Entry<String, WebRequestMetricSet> entry : metricsMap.entrySet()) {
            metricsMap.compute(entry.getKey(),
                               (k,
                                v) -> getAndRemove(v));
            messages.add(messageConverter.from(timestamp,
                                               interval,
                                               this.metric));
        }
        return messages;
    }

    private WebRequestMetricSet getAndRemove(WebRequestMetricSet counter) {
        this.metric = counter;
        return null;
    }
}
