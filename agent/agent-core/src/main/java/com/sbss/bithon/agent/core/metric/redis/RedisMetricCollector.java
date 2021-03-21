package com.sbss.bithon.agent.core.metric.redis;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.IMetricCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 */
public class RedisMetricCollector implements IMetricCollector {

    private final Map<RedisMetricDimension, RedisClientMetric> metricsMap = new ConcurrentHashMap<>();
    private RedisClientMetric metric;

    public void addWrite(String hostAndPort,
                         String command,
                         long costTime,
                         boolean hasException) {
        int exceptionCount = hasException ? 1 : 0;

        RedisClientMetric redisClientMetric = metricsMap.computeIfAbsent(new RedisMetricDimension(hostAndPort, command),
                                                             k -> new RedisClientMetric(hostAndPort, command));
        redisClientMetric.addRequest(costTime, exceptionCount);
    }

    public void addRead(String hostAndPort,
                        String command,
                        long costTime,
                        boolean hasException) {
        int exceptionCount = hasException ? 1 : 0;

        RedisClientMetric redisClientMetric = metricsMap.computeIfAbsent(new RedisMetricDimension(hostAndPort, command),
                                                             k -> new RedisClientMetric(hostAndPort, command));
        redisClientMetric.addResponse(costTime, exceptionCount);
    }

    public void addOutputBytes(String hostAndPort,
                               String command,
                               int bytesOut) {
        metricsMap.computeIfAbsent(new RedisMetricDimension(hostAndPort, command),
                                   k -> new RedisClientMetric(hostAndPort, command))
                  .addRequestBytes(bytesOut);
    }

    public void addInputBytes(String hostAndPort,
                              String command,
                              int bytesIn) {
        metricsMap.computeIfAbsent(new RedisMetricDimension(hostAndPort, command),
                                   k -> new RedisClientMetric(hostAndPort, command))
                  .addResponseBytes(bytesIn);
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
        for (Map.Entry<RedisMetricDimension, RedisClientMetric> entry : metricsMap.entrySet()) {
            metricsMap.compute(entry.getKey(),
                               (k,
                                v) -> v == null ? null : getAndRemove(v));

            messages.add(messageConverter.from(timestamp, interval, this.metric));
        }
        return messages;
    }

    private RedisClientMetric getAndRemove(RedisClientMetric redisClientMetric) {
        metric = redisClientMetric;
        return null;
    }

    static class RedisMetricDimension {
        private final String hostAndPort;
        private final String command;

        RedisMetricDimension(String hostAndPort, String command) {
            this.hostAndPort = hostAndPort;
            this.command = command;
        }

        @Override
        public int hashCode() {
            return Objects.hash(hostAndPort, command);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RedisMetricDimension) {
                RedisMetricDimension that = (RedisMetricDimension) obj;
                return this.hostAndPort.equals(that.hostAndPort)
                       && this.command.equals(that.command);
            } else {
                return false;
            }
        }
    }
}
