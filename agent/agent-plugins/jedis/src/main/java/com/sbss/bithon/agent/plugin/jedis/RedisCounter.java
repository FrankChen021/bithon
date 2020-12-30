package com.sbss.bithon.agent.plugin.jedis;

import com.keruyun.commons.agent.collector.entity.MiddlewareEntity;
import com.keruyun.commons.agent.collector.entity.RedisEntity;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.util.ReflectUtil;
import com.sbss.bithon.agent.dispatcher.metrics.counter.AgentCounterRepository;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;
import com.sbss.bithon.agent.dispatcher.metrics.redis.RedisMetrics;
import redis.clients.jedis.Client;
import redis.clients.jedis.Connection;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description : Jedis - 记录器, 设计为单例以全局性的获取请求状态 <br>
 * Date: 17/10/30
 *
 * @author 马至远
 */
public class RedisCounter implements IAgentCounter {
    private static final Logger log = LoggerFactory.getLogger(RedisCounter.class);

    static final String KEY_BEGIN_TIME = "beginProcessingTime";

    private static final String COUNTER_NAME = "jedis";

    /**
     * 计数器内部存储, String存放 host + port
     */
    private Map<String, RedisMetrics> redisStorages = new ConcurrentHashMap<>();

    private Map<String, ConnectionStreamsMapping> redisConnectionStreamsMapping = new ConcurrentHashMap<>();

    class ConnectionStreamsMapping {
        Object redisInputStream;
        Object redisOutputStream;

        Object getRedisOutputStream() {
            return redisOutputStream;
        }

        Object getRedisInputStream() {
            return redisInputStream;
        }

        /**
         * 绑定connection与streams的映射关系
         *
         * @param inputStream  redis input stream
         * @param outputStream redis output stream
         */
        void bandStreams(Object inputStream,
                         Object outputStream) {
            this.redisInputStream = inputStream;
            this.redisOutputStream = outputStream;
        }
    }

    private static class RedisCounterHolder {
        static final RedisCounter INSTANCE = new RedisCounter();
    }

    private RedisCounter() {
        // 获取CounterRepository 实例
        AgentCounterRepository counterRepository = AgentCounterRepository.getInstance();

        // 向counterRepository注册自身, 开始统计druid连接池信息
        try {
            counterRepository.register(COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("jedis counter init failed due to ", e);
        }
    }

    private RedisMetrics tempCounterStorage;

    static RedisCounter getInstance() {
        return RedisCounterHolder.INSTANCE;
    }

    /**
     * 由于统计点分散, 所以不使用此方法
     *
     * @see #parseConnection(AfterJoinPoint)
     */
    @Override
    @Deprecated
    public void add(Object o) {
    }

    /**
     * 解析jedis connection内容
     *
     * @param joinPoint join point
     */
    void parseConnection(AfterJoinPoint joinPoint) {
        Connection connection = (Connection) joinPoint.getTarget();
        parseRedisAndStreamMapping(connection, joinPoint);
    }

    /**
     * 统计jedis clients的请求次数相关
     */
    public void countRequestNum(Client client,
                                boolean hasException,
                                long startTime,
                                boolean isRead) {
        long costTime = System.nanoTime() - startTime;
        int failureCount = hasException ? 1 : 0;

        String hostPort = client.getHost().concat(":").concat(String.valueOf(client.getPort()));

        // 尝试记录新的redis连接
        RedisMetrics redisMetrics = redisStorages.computeIfAbsent(hostPort,
                                                                  k -> new RedisMetrics(hostPort));
        if (isRead) {
            redisMetrics.addRead(costTime, failureCount);
        } else {
            redisMetrics.addWrite(costTime, failureCount);
        }
    }

    /**
     * 解析connection和streams的映射关系
     *
     * @param connection     redis connection
     * @param afterJoinPoint after join point
     */
    private void parseRedisAndStreamMapping(Connection connection,
                                            AfterJoinPoint afterJoinPoint) {
        if (afterJoinPoint.getException() != null) {
            return;
        }

        String hostPort = connection.getHost().concat(":").concat(String.valueOf(connection.getPort()));

        Object inputStream = ReflectUtil.getFieldValue(connection, "inputStream");
        Object outputStream = ReflectUtil.getFieldValue(connection, "outputStream");

        if (redisConnectionStreamsMapping.containsKey(hostPort)) {
            redisConnectionStreamsMapping.get(hostPort).bandStreams(inputStream, outputStream);
        } else {
            redisConnectionStreamsMapping.computeIfAbsent(hostPort, k -> {
                ConnectionStreamsMapping connectionStreamsMapping = new ConnectionStreamsMapping();
                connectionStreamsMapping.bandStreams(inputStream, outputStream);
                return connectionStreamsMapping;
            });
        }
    }

    /**
     * 解析bytes out
     *
     * @param redisOutputStream redis output stream
     */
    public void countOutputBytes(Object redisOutputStream,
                                 int bytesOut) {
        String key = findStreamMappingConnection(redisOutputStream);
        if (null == key) {
            return;
        }

        try {
            RedisMetrics s = redisStorages.get(key);
            if (s != null) {
                s.addBytesOut(bytesOut);
            }
        } catch (Exception e) {
            log.warn("redis byte got unexpected exception", e);
        }
    }

    /**
     * 解析bytes in
     *
     * @param inputStream redis input stream
     */
    public void countInputBytes(Object inputStream,
                                int bytesIn) {
        String key = findStreamMappingConnection(inputStream);
        if (null == key) {
            return;
        }

        try {
            RedisMetrics s = redisStorages.get(key);
            if (s != null) {
                s.addBytesIn(bytesIn);
            }
        } catch (Exception e) {
            log.error("redis byte got unexpected exception", e);
        }
    }

    private String findStreamMappingConnection(Object stream) {
        for (Map.Entry<String, ConnectionStreamsMapping> entry : redisConnectionStreamsMapping.entrySet()) {
            if (stream.equals(entry.getValue().getRedisInputStream()) ||
                stream.equals(entry.getValue().getRedisOutputStream())) {
                return entry.getKey();
            }
        }

        return null;
    }

    @Override
    public boolean isEmpty() {
        return redisStorages.isEmpty();
    }

    @Override
    public List<?> buildAndGetThriftEntities(int interval,
                                             String appName,
                                             String ipAddress,
                                             int port) {
        return buildEntities(interval, appName, ipAddress, port);
    }

    /**
     * 从当前storage中构建thrift数据
     *
     * @return agent采集数据
     */
    private List<MiddlewareEntity> buildEntities(int interval,
                                                 String appName,
                                                 String ipAddress,
                                                 int port) {
        List<MiddlewareEntity> redisEntities = new ArrayList<>();

        for (Map.Entry<String, RedisMetrics> entry : redisStorages.entrySet()) {
            redisStorages.compute(entry.getKey(),
                                  (k,
                                   v) -> v == null ? null : tempAndRemoveEntry(v));
            MiddlewareEntity redisEntity = new MiddlewareEntity(null,
                                                                new RedisEntity(appName,
                                                                                ipAddress,
                                                                                port,
                                                                                tempCounterStorage.getHostPort(),
                                                                                System.currentTimeMillis(),
                                                                                interval,
                                                                                tempCounterStorage.getDb(),
                                                                                tempCounterStorage.getCount().get(),
                                                                                tempCounterStorage.getFailureCount()
                                                                                    .get(),
                                                                                tempCounterStorage.getReadCostTime()
                                                                                    .get(),
                                                                                tempCounterStorage.getWriteCostTime()
                                                                                    .get(),
                                                                                tempCounterStorage.getBytesOut().get(),
                                                                                tempCounterStorage.getBytesIn().get()),
                                                                null,
                                                                null);
            redisEntities.add(redisEntity);
        }

        return redisEntities;
    }

    private RedisMetrics tempAndRemoveEntry(RedisMetrics redisMetrics) {
        tempCounterStorage = redisMetrics;
        return new RedisMetrics(redisMetrics.getHostPort());
    }
}
