package com.sbss.bithon.agent.core.metric.domain.redis;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IntervalMetricCollector;

import java.util.List;

/**
 * @author frankchen
 */
public class RedisMetricCollector extends IntervalMetricCollector<RedisClientCompositeMetric> {

    public void addWrite(String endpoint,
                         String command,
                         long responseTime,
                         boolean hasException) {
        int exceptionCount = hasException ? 1 : 0;

        getOrCreateMetric(endpoint, command).addRequest(responseTime, exceptionCount);
    }

    public void addRead(String endpoint,
                        String command,
                        long responseTime,
                        boolean hasException) {
        int exceptionCount = hasException ? 1 : 0;

        getOrCreateMetric(endpoint, command).addResponse(responseTime, exceptionCount);
    }

    public void addOutputBytes(String endpoint,
                               String command,
                               int bytesOut) {
        getOrCreateMetric(endpoint, command).addRequestBytes(bytesOut);
    }

    public void addInputBytes(String endpoint,
                              String command,
                              int bytesIn) {
        getOrCreateMetric(endpoint, command).addResponseBytes(bytesIn);
    }

    @Override
    protected RedisClientCompositeMetric newMetrics() {
        return new RedisClientCompositeMetric();
    }

    @Override
    protected Object toMessage(IMessageConverter messageConverter,
                               int interval,
                               long timestamp,
                               List<String> dimensions,
                               RedisClientCompositeMetric metric) {
        return messageConverter.from(timestamp, interval, dimensions, metric);
    }
}
