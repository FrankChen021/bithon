package com.sbss.bithon.agent.dispatcher.metrics.counter;

import com.keruyun.commons.agent.collector.entity.MiddlewareEntity;
import com.keruyun.commons.agent.collector.entity.MongoDBEntity;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author huangchen
 */
public class MongoDbCounter implements IAgentCounter {
    private static final Logger log = LoggerFactory.getLogger(MongoDbCounter.class);

    private static final String COUNTER_NAME = "mongodb";

    /**
     * 计数器内部存储, String存放host + port
     */
    private Map<String, MongoDbCounterStorage> mongoDbCounterStorageMap = new ConcurrentHashMap<>();

    /**
     * 存储mongoDb Id connectionId到instance的映射关系, 官方api说此id是不可变的, 但是connection销毁后, 会不会出现新的connectionId 到instance的映射, 有待观察
     */
    private Map<String, String> mongoDbConnectionIdHostPortMapping = new ConcurrentHashMap<>();

    private final class MongoDbCounterStorage {
        /**
         * host + port
         */
        String hostPort;
        /**
         * commands total costTime
         */
        AtomicLong costTime = new AtomicLong(0);
        /**
         * commands count
         */
        AtomicInteger commands = new AtomicInteger(0);
        /**
         * commands failure count
         */
        AtomicInteger failureCount = new AtomicInteger(0);
        /**
         * bytes in
         */
        AtomicLong bytesIn = new AtomicLong(0);
        /**
         * bytes out
         */
        AtomicLong bytesOut = new AtomicLong(0);

        MongoDbCounterStorage(String hostPort) {
            this.hostPort = hostPort;
        }

        void add(long costTime, int failureCount) {
            this.commands.incrementAndGet();
            this.costTime.addAndGet(costTime);
            this.failureCount.addAndGet(failureCount);
        }

        void addBytesIn(int bytesIn) {
            this.bytesIn.addAndGet(bytesIn);
        }

        void addBytesOut(int bytesOut) {
            this.bytesOut.addAndGet(bytesOut);
        }

        String getHostPort() {
            return hostPort;
        }

        long getAndClearCostTime() {
            return costTime.getAndSet(0);
        }

        int getAndClearCommands() {
            return commands.getAndSet(0);
        }

        int getAndClearFailureCount() {
            return failureCount.getAndSet(0);
        }

        long getAndClearBytesIn() {
            return bytesIn.getAndSet(0);
        }

        long getAndClearBytesOut() {
            return bytesOut.getAndSet(0);
        }
    }

    private MongoDbCounterStorage tempCounterStorage;

    private MongoDbCounter() {
        // 获取CounterRepository 实例
        AgentCounterRepository counterRepository = AgentCounterRepository.getInstance();

        // 向counterRepository注册自身, 开始统计druid连接池信息
        try {
            counterRepository.register(COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("mongodb counter init failed due to ", e);
        }
    }

    private static class MongoDbCounterHolder {
        static final MongoDbCounter INSTANCE = new MongoDbCounter();
    }

    public static MongoDbCounter getInstance() {
        return MongoDbCounterHolder.INSTANCE;
    }

    @Override
    public void add(Object o) {
    }


    public void recordBytesOut(String connectionId, int bytesOut) {
        String hostPort = mongoDbConnectionIdHostPortMapping.get(connectionId);

        if (hostPort != null) {
            MongoDbCounterStorage mongoDbCounterStorage = mongoDbCounterStorageMap.get(hostPort);
            if (mongoDbCounterStorage != null) {
                log.debug("app-mongodb-debugging: bytesout=" + bytesOut);
                mongoDbCounterStorage.addBytesIn(bytesOut);
            }
        }
    }

    public void recordBytesIn(String connectionId, int bytesIn) {
        if (connectionId == null) {
            return;
        }

        String hostPort = mongoDbConnectionIdHostPortMapping.get(connectionId);

        if (hostPort != null) {
            MongoDbCounterStorage mongoDbCounterStorage = mongoDbCounterStorageMap.get(hostPort);
            if (mongoDbCounterStorage != null) {
                log.debug("app-mongodb-debugging: bytesin=" + bytesIn);
                mongoDbCounterStorage.addBytesOut(bytesIn);
            }
        }
    }


    public void recordRequestInfo(String connectionId, String hostPort, Long costTime, int failureCount) {
        // 尝试记录新的mongoDB连接
        MongoDbCounterStorage mongoDbCounterStorage = mongoDbCounterStorageMap.computeIfAbsent(hostPort, k -> new MongoDbCounterStorage(hostPort));
        mongoDbCounterStorage.add(costTime, failureCount);

        // 写入映射关系
        mongoDbConnectionIdHostPortMapping.putIfAbsent(connectionId, hostPort);
    }

    @Override
    public boolean isEmpty() {
        return mongoDbCounterStorageMap.isEmpty();
    }

    @Override
    public List buildAndGetThriftEntities(int interval, String appName, String ipAddress, int port) {
        return buildEntities(interval, appName, ipAddress, port);
    }

    /**
     * 从当前storage中构建thrift数据
     *
     * @return agent采集数据
     */
    private List<MiddlewareEntity> buildEntities(int interval, String appName, String ipAddress, int port) {
        if (log.isDebugEnabled()) {
            log.debug("app-mongodb-debugging: Current Mongo ConnectionId Mapping: " + mongoDbConnectionIdHostPortMapping.toString());
        }

        List<MiddlewareEntity> mongoDbEntities = new ArrayList<>();

        for (Map.Entry<String, MongoDbCounterStorage> entry : mongoDbCounterStorageMap.entrySet()) {
            mongoDbCounterStorageMap.compute(entry.getKey(), (k, v) -> tempAndRemoveEntry(v));
            MiddlewareEntity mongoDbEntity = new MiddlewareEntity(null, null, new MongoDBEntity(
                appName,
                ipAddress,
                port,
                tempCounterStorage.getHostPort(),
                System.currentTimeMillis(),
                interval,
                tempCounterStorage.getAndClearCommands(),
                tempCounterStorage.getAndClearFailureCount(),
                tempCounterStorage.getAndClearCostTime(),
                tempCounterStorage.getAndClearBytesOut(),
                tempCounterStorage.getAndClearBytesIn()
            ), null);
            mongoDbEntities.add(mongoDbEntity);
        }

        return mongoDbEntities;
    }

    private MongoDbCounterStorage tempAndRemoveEntry(MongoDbCounterStorage mongoDbCounterStorage) {
        tempCounterStorage = mongoDbCounterStorage;
        // 这里在临时存放后, 返回null, 目的是让map清除当前计算的值, 开始新的统计
        return null;
    }
}
