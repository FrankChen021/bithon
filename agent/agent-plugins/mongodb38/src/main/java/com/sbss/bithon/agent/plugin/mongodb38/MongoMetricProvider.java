package com.sbss.bithon.agent.plugin.mongodb38;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.IMetricProvider;
import com.sbss.bithon.agent.core.metric.MetricProviderManager;
import com.sbss.bithon.agent.core.metric.mongo.MongoMetric;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 */
public class MongoMetricProvider implements IMetricProvider {
    static final MongoMetricProvider INSTANCE = new MongoMetricProvider();
    private static final Logger log = LoggerFactory.getLogger(MongoMetricProvider.class);
    private static final String COUNTER_NAME = "mongodb38";
    /**
     * 计数器内部存储, String存放host + port
     */
    private final Map<String, MongoMetric> mongoDbCounterStorageMap = new ConcurrentHashMap<>();
    /**
     * 存储mongoDb Id connectionId到instance的映射关系, 官方api说此id是不可变的, 但是connection销毁后, 会不会出现新的connectionId 到instance的映射, 有待观察
     */
    private final Map<String, String> mongoDbConnectionIdHostPortMapping = new ConcurrentHashMap<>();
    private MongoMetric counter;

    private MongoMetricProvider() {
        try {
            MetricProviderManager.getInstance().register(COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("mongodb counter init failed due to ", e);
        }
    }

    public static MongoMetricProvider getInstance() {
        return INSTANCE;
    }

    public void recordBytesOut(String connectionId, int bytesOut) {
        String hostPort = mongoDbConnectionIdHostPortMapping.get(connectionId);

        if (hostPort != null) {
            MongoMetric mongoMetricStorage = mongoDbCounterStorageMap.get(hostPort);
            if (mongoMetricStorage != null) {
                log.debug("app-mongodb-debugging: bytesOut=" + bytesOut);
                mongoMetricStorage.addBytesIn(bytesOut);
            }
        }
    }

    public void recordBytesIn(String connectionId, int bytesIn) {
        if (connectionId == null) {
            return;
        }

        String hostPort = mongoDbConnectionIdHostPortMapping.get(connectionId);

        if (hostPort != null) {
            MongoMetric mongoMetricStorage = mongoDbCounterStorageMap.get(hostPort);
            if (mongoMetricStorage != null) {
                log.debug("app-mongodb-debugging: bytesIn=" + bytesIn);
                mongoMetricStorage.addBytesOut(bytesIn);
            }
        }
    }

    public void recordRequestInfo(String connectionId, String hostPort, Long costTime, int failureCount) {
        mongoDbCounterStorageMap.computeIfAbsent(hostPort, k -> new MongoMetric(hostPort))
                                .add(costTime, failureCount);

        // save mapping
        mongoDbConnectionIdHostPortMapping.putIfAbsent(connectionId, hostPort);
    }

    @Override
    public boolean isEmpty() {
        return mongoDbCounterStorageMap.isEmpty();
    }

    @Override
    public List<Object> buildMessages(IMessageConverter messageConverter,
                                      AppInstance appInstance,
                                      int interval,
                                      long timestamp) {
        if (log.isDebugEnabled()) {
            log.debug("app-mongodb-debugging: Current Mongo ConnectionId Mapping: " + mongoDbConnectionIdHostPortMapping
                .toString());
        }

        List<Object> messages = new ArrayList<>();
        for (Map.Entry<String, MongoMetric> entry : mongoDbCounterStorageMap.entrySet()) {
            mongoDbCounterStorageMap.compute(entry.getKey(), (k, v) -> getAndRemove(v));

            messages.add(messageConverter.from(appInstance, timestamp, interval, this.counter));
        }
        return messages;
    }

    private MongoMetric getAndRemove(MongoMetric counter) {
        this.counter = counter;
        return null;
    }
}
