package com.sbss.bithon.agent.plugin.mongodb;

import com.mongodb.connection.Connection;
import com.mongodb.connection.ConnectionId;
import com.mongodb.event.ConnectionMessageReceivedEvent;
import com.mongodb.event.ConnectionMessagesSentEvent;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.metrics.IMetricProvider;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.metrics.mongo.MongoMetric;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 */
public class MongoMetricProvider implements IMetricProvider {
    private static final Logger log = LoggerFactory.getLogger(MongoMetricProvider.class);

    private static final String COUNTER_NAME = "mongodb";

    /**
     * 计数器内部存储, String存放host + port
     */
    private final Map<String, MongoMetric> counters = new ConcurrentHashMap<>();

    /**
     * 存储mongoDb Id connectionId到instance的映射关系, 官方api说此id是不可变的, 但是connection销毁后,
     * 会不会出现新的connectionId 到instance的映射, 有待观察
     */
    private final Map<ConnectionId, String> mongoDbConnectionIdHostPortMapping = new HashMap<>();

    private MongoMetric counter;

    private MongoMetricProvider() {
        try {
            MetricProviderManager.getInstance().register(COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("mongodb counter init failed due to ", e);
        }
    }

    static final MongoMetricProvider INSTANCE = new MongoMetricProvider();

    static MongoMetricProvider getInstance() {
        return INSTANCE;
    }

    public void update(Object o) {
        AopContext aopContext = (AopContext) o;

        if (aopContext.getTarget() instanceof ConnectionMessagesSentEvent) {
            // 记录bytes out
            countBytesOut(aopContext);
        } else if (aopContext.getTarget() instanceof ConnectionMessageReceivedEvent) {
            // 记录bytes in
            countBytesIn(aopContext);
        } else {
            // 记录请求, 尝试建立connectionId - hostPort映射关系
            countRequestInfo(aopContext);
        }
    }

    private void countRequestInfo(AopContext aopContext) {
        Connection connection = (Connection) aopContext.getTarget();
        String hostPort = connection.getDescription().getServerAddress().toString();
        int failureCount = null == aopContext.getException() ? 0 : 1;

        // 尝试记录新的mongoDB连接
        MongoMetric mongoMetricStorage = counters.computeIfAbsent(hostPort,
                                                                        k -> new MongoMetric(hostPort));
        mongoMetricStorage.add(aopContext.getCostTime(), failureCount);

        // 写入映射关系
        mongoDbConnectionIdHostPortMapping.putIfAbsent(connection.getDescription().getConnectionId(), hostPort);
    }

    private void countBytesIn(AopContext aopContext) {
        int bytesIn = (int) aopContext.getArgs()[2];
        ConnectionId connectionId = (ConnectionId) aopContext.getArgs()[0];
        String hostPort = mongoDbConnectionIdHostPortMapping.get(connectionId);

        if (hostPort != null) {
            MongoMetric mongoMetricStorage = counters.get(hostPort);
            if (mongoMetricStorage != null) {
                log.debug("app-mongodb-debugging: bytesIn=" + bytesIn);
                mongoMetricStorage.addBytesIn(bytesIn);
            }
        }
    }

    private void countBytesOut(AopContext aopContext) {
        int bytesOut = (int) aopContext.getArgs()[2];
        ConnectionId connectionId = (ConnectionId) aopContext.getArgs()[0];
        String hostPort = mongoDbConnectionIdHostPortMapping.get(connectionId);

        if (hostPort != null) {
            MongoMetric mongoMetricStorage = counters.get(hostPort);
            if (mongoMetricStorage != null) {
                log.debug("app-mongodb-debugging: bytesOut=" + bytesOut);
                mongoMetricStorage.addBytesOut(bytesOut);
            }
        }
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
        log.debug("app-mongodb-debugging: Current Mongo ConnectionId Mapping: " +
                      mongoDbConnectionIdHostPortMapping.toString());

        List<Object> messages = new ArrayList<>();
        for (Map.Entry<String, MongoMetric> entry : counters.entrySet()) {
            counters.compute(entry.getKey(),
                             (k,
                              v) -> getAndRemove(v));
            messages.add(messageConverter.from(appInstance, timestamp, interval, this.counter));
        }
        return messages;
    }

    private MongoMetric getAndRemove(MongoMetric counter) {
        this.counter = counter;
        return null;
    }
}
