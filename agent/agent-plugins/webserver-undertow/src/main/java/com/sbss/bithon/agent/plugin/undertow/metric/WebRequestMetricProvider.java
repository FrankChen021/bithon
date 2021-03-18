package com.sbss.bithon.agent.plugin.undertow.metric;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.IMetricProvider;
import com.sbss.bithon.agent.core.metric.MetricProviderManager;
import com.sbss.bithon.agent.core.metric.web.WebRequestMetric;
import io.undertow.server.HttpServerExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 */
public class WebRequestMetricProvider implements IMetricProvider {

    private WebRequestMetric metric;
    private final Map<String, WebRequestMetric> metricsMap = new ConcurrentHashMap<>();

    private static final WebRequestMetricProvider INSTANCE = new WebRequestMetricProvider();

    public static WebRequestMetricProvider getInstance() {
        return INSTANCE;
    }

    WebRequestMetricProvider() {
        MetricProviderManager.getInstance().register("undertow-webRequest", this);
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

        WebRequestMetric counter = metricsMap.computeIfAbsent(srcApplication + "|" + uri, key -> new WebRequestMetric(srcApplication, uri));
        counter.add(costTime, errorCount, count4xx, count5xx);
        counter.addBytes(requestByteSize, responseByteSize);
    }

    @Override
    public boolean isEmpty() {
        return metricsMap.isEmpty();
    }

    @Override
    public List<Object> buildMessages(IMessageConverter messageConverter,
                                      AppInstance appInstance,
                                      int interval,
                                      long timestamp) {
        List<Object> messages = new ArrayList<>();
        for (Map.Entry<String, WebRequestMetric> entry : metricsMap.entrySet()) {
            metricsMap.compute(entry.getKey(),
                               (k,
                              v) -> getAndRemove(v));
            messages.add(messageConverter.from(appInstance,
                                               timestamp,
                                               interval,
                                               this.metric));
        }
        return messages;
    }

    private WebRequestMetric getAndRemove(WebRequestMetric counter) {
        this.metric = counter;
        return null;
    }
}
