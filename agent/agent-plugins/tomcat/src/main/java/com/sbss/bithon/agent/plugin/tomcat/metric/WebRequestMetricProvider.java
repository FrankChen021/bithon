package com.sbss.bithon.agent.plugin.tomcat.metric;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metrics.IMetricProvider;
import com.sbss.bithon.agent.core.metrics.web.WebRequestMetric;
import org.apache.coyote.Request;
import org.apache.coyote.Response;

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

    public WebRequestMetricProvider() {
    }

    public void update(Request request, Response response, long costTime) {
        String uri = request.requestURI().toString();
        if (uri == null) {
            return;
        }

        int httpStatus = response.getStatus();
        int errorCount = response.getStatus() >= 400 ? 1 : 0;
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 && httpStatus < 600 ? 1 : 0;
        long requestByteSize = request.getBytesRead();
        long responseByteSize = response.getBytesWritten(false);

        WebRequestMetric metric = metricsMap.computeIfAbsent(uri, WebRequestMetric::new);
        metric.add(costTime, errorCount, count4xx, count5xx);
        metric.addByteSize(requestByteSize, responseByteSize);
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

            messages.add(messageConverter.from(appInstance, timestamp, interval, this.metric));
        }
        return messages;
    }

    private WebRequestMetric getAndRemove(WebRequestMetric metric) {
        this.metric = metric;
        return null;
    }
}
