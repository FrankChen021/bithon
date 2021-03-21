package com.sbss.bithon.agent.plugin.mongodb38;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.IMetricCollector;
import com.sbss.bithon.agent.core.metric.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.mongo.MongoDbMetricSet;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 */
public class MongoMetricCollector implements IMetricCollector {
    static final MongoMetricCollector INSTANCE = new MongoMetricCollector();
    private static final Logger log = LoggerFactory.getLogger(MongoMetricCollector.class);
    private static final String COUNTER_NAME = "mongodb38";
    /**
     * 计数器内部存储, String存放host + port
     */
    private final Map<String, MongoDbMetricSet> metricMap = new ConcurrentHashMap<>();
    /**
     * 存储mongoDb Id connectionId到instance的映射关系, 官方api说此id是不可变的, 但是connection销毁后, 会不会出现新的connectionId 到instance的映射, 有待观察
     */
    private final Map<String, String> mongoDbConnectionIdHostPortMapping = new ConcurrentHashMap<>();
    private MongoDbMetricSet metricSet;

    private MongoMetricCollector() {
        try {
            MetricCollectorManager.getInstance().register(COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("mongodb counter init failed due to ", e);
        }
    }

    public static MongoMetricCollector getInstance() {
        return INSTANCE;
    }

    public void recordBytesOut(String connectionId, int bytesOut) {
        String hostPort = mongoDbConnectionIdHostPortMapping.get(connectionId);

        if (hostPort != null) {
            MongoDbMetricSet metricSet = metricMap.get(hostPort);
            if (metricSet != null) {
                log.debug("app-mongodb-debugging: bytesOut=" + bytesOut);
                metricSet.addBytesIn(bytesOut);
            }
        }
    }

    public void recordBytesIn(String connectionId, int bytesIn) {
        if (connectionId == null) {
            return;
        }

        String hostPort = mongoDbConnectionIdHostPortMapping.get(connectionId);

        if (hostPort != null) {
            MongoDbMetricSet metricSet = metricMap.get(hostPort);
            if (metricSet != null) {
                log.debug("app-mongodb-debugging: bytesIn=" + bytesIn);
                metricSet.addBytesOut(bytesIn);
            }
        }
    }

    public void recordRequestInfo(String connectionId, String hostPort, Long costTime, int failureCount) {
        metricMap.computeIfAbsent(hostPort, k -> new MongoDbMetricSet(hostPort))
                 .add(costTime, failureCount);

        // save mapping
        mongoDbConnectionIdHostPortMapping.putIfAbsent(connectionId, hostPort);
    }

    @Override
    public boolean isEmpty() {
        return metricMap.isEmpty();
    }

    @Override
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {
        if (log.isDebugEnabled()) {
            log.debug("app-mongodb-debugging: Current Mongo ConnectionId Mapping: " + mongoDbConnectionIdHostPortMapping
                .toString());
        }

        List<Object> messages = new ArrayList<>();
        for (Map.Entry<String, MongoDbMetricSet> entry : metricMap.entrySet()) {
            metricMap.compute(entry.getKey(), (k, v) -> getAndRemove(v));

            messages.add(messageConverter.from(timestamp, interval, this.metricSet));
        }
        return messages;
    }

    private MongoDbMetricSet getAndRemove(MongoDbMetricSet metricSet) {
        this.metricSet = metricSet;
        return null;
    }
}
