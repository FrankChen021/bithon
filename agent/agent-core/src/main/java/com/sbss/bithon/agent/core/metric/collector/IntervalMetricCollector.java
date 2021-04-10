package com.sbss.bithon.agent.core.metric.collector;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.model.IMetric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This collector clear metrics after it collect metrics.
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/25 4:37 下午
 */
public abstract class IntervalMetricCollector<T extends IMetric> implements IMetricCollector {

    private Map<List<String>, T> metricsMap = new ConcurrentHashMap<>();

    protected T getOrCreateMetric(String... dimensionValues) {
        List<String> dimensions = Arrays.asList(dimensionValues);
        return metricsMap.computeIfAbsent(dimensions, key -> newMetrics());
    }

    @Override
    public boolean isEmpty() {
        return metricsMap.isEmpty();
    }

    @Override
    public List<Object> collect(IMessageConverter messageConverter, int interval, long timestamp) {
        //swap metrics
        Map<List<String>, T> metrics = metricsMap;
        metricsMap = new ConcurrentHashMap<>();

        List<Object> messages = new ArrayList<>(metrics.size());
        for (Map.Entry<List<String>, T> metricEntry : metrics.entrySet()) {
            Object message = toMessage(messageConverter,
                                       interval,
                                       timestamp,
                                       metricEntry.getKey(),
                                       metricEntry.getValue());
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    protected abstract T newMetrics();

    protected abstract Object toMessage(IMessageConverter messageConverter,
                                        int interval,
                                        long timestamp,
                                        List<String> dimensions,
                                        T metric);
}
