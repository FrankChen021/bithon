package com.sbss.bithon.agent.plugin.mysql;

import com.keruyun.commons.agent.collector.entity.SqlInfoEntity;
import com.keruyun.commons.agent.collector.entity.SqlPerformanceEntity;
import com.mysql.jdbc.Buffer;
import com.mysql.jdbc.MysqlIO;
import com.mysql.jdbc.ResultSetImpl;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.util.ReflectUtil;
import com.sbss.bithon.agent.dispatcher.metrics.counter.AgentCounterRepository;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Description : mysql 执行计数器 <br>
 * Date: 17/10/31
 *
 * @author 马至远
 */
public class MySqlCounter implements IAgentCounter {
    private static final Logger log = LoggerFactory.getLogger(MySqlCounter.class);

    static final String KEY_BEGIN_TIME = "beginExecutingTime";

    private static final String MYSQL_COUNTER_NAME = "mysql";
    private static final String DRIVER_TYPE_MYSQL = "mysql";

    private Map<String, MysqlCounterStorage> mysqlCounterStorageMap;

    private final class MysqlCounterStorage {
        private AtomicLong totalCostTime = new AtomicLong();
        private AtomicInteger totalFailureCount = new AtomicInteger();
        private AtomicInteger totalCount = new AtomicInteger();
        private AtomicInteger totalQueryCount = new AtomicInteger();
        private AtomicInteger totalUpdateCount = new AtomicInteger();
        private AtomicLong totalBytesIn = new AtomicLong();
        private AtomicLong totalBytesOut = new AtomicLong();

        MysqlCounterStorage() {
        }

        void add(boolean isQuery,
                 boolean failed,
                 long costTime) {
            this.totalCostTime.addAndGet(costTime);
            if (isQuery) {
                this.totalQueryCount.incrementAndGet();
            } else {
                this.totalUpdateCount.incrementAndGet();
            }

            if (failed) {
                this.totalFailureCount.incrementAndGet();
            }

            this.totalCount.incrementAndGet();
        }

        void addBytesIn(int bytesIn) {
            this.totalBytesIn.addAndGet(bytesIn);
        }

        void addBytesOut(int bytesOut) {
            this.totalBytesOut.addAndGet(bytesOut);
        }

        long getAndClearTotalCostTime() {
            return totalCostTime.getAndSet(0);
        }

        int getAndClearTotalFailureCount() {
            return totalFailureCount.getAndSet(0);
        }

        int getAndClearTotalCount() {
            return totalCount.getAndSet(0);
        }

        int getAndClearTotalQueryCount() {
            return totalQueryCount.getAndSet(0);
        }

        int getAndClearTotalUpdateCount() {
            return totalUpdateCount.getAndSet(0);
        }

        long getAndClearTotalBytesIn() {
            return totalBytesIn.getAndSet(0);
        }

        long getAndClearTotalBytesOut() {
            return totalBytesOut.getAndSet(0);
        }
    }

    private static class MySqlCounterHolder {
        static final MySqlCounter INSTANCE = new MySqlCounter();
    }

    static MySqlCounter getInstance() {
        return MySqlCounterHolder.INSTANCE;
    }

    private MySqlCounter() {
        mysqlCounterStorageMap = new ConcurrentHashMap<>();

        // 获取CounterRepository 实例
        AgentCounterRepository counterRepository = AgentCounterRepository.getInstance();

        // 向counterRepository注册自身, 开始统计druid连接池信息
        try {
            counterRepository.register(MYSQL_COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("druid counter init failed due to ", e);
        }
    }

    @Override
    public void add(Object o) {
        AfterJoinPoint afterJoinPoint = (AfterJoinPoint) o;
        long costTime = System.nanoTime() - (Long) afterJoinPoint.getContext();

        if (afterJoinPoint.getTarget() instanceof Statement) {
            countExecutedNum(afterJoinPoint, costTime);
        } else {
            countBytesNum(afterJoinPoint);
        }
    }

    private void countBytesNum(AfterJoinPoint afterJoinPoint) {
        String methodName = afterJoinPoint.getMethod().getName();
        try {
            MysqlIO mysqlIO = (MysqlIO) afterJoinPoint.getTarget();

            // 反射式的获取当前mysqlIO host+port
            String host = (String) ReflectUtil.getFieldValue(mysqlIO, "host");
            Integer port = (Integer) ReflectUtil.getFieldValue(mysqlIO, "port");
            String hostPort = host + ":" + port;

            // 尝试记录新的mysql连接
            MysqlCounterStorage mysqlCounterStorage = mysqlCounterStorageMap.computeIfAbsent(hostPort,
                                                                                             k -> new MysqlCounterStorage());

            if (MySqlTransformer.METHOD_SEND_COMMAND.equals(methodName)) {
                Buffer queryPacket = (Buffer) afterJoinPoint.getArgs()[2];
                if (queryPacket != null) {
                    mysqlCounterStorage.addBytesOut(queryPacket.getPosition());
                }
            } else {
                ResultSetImpl resultSet = (ResultSetImpl) afterJoinPoint.getResult();
                int bytesIn = resultSet.getBytesSize();
                if (bytesIn > 0) {
                    mysqlCounterStorage.addBytesIn(resultSet.getBytesSize());
                }
            }
        } catch (Exception e) {
            log.error("get bytes info error", e);
        }
    }

    private void countExecutedNum(AfterJoinPoint afterJoinPoint,
                                  long costTime) {
        String methodName = afterJoinPoint.getMethod().getName();

        Statement statement = (Statement) afterJoinPoint.getTarget();
        try {
            String hostPort = parseDBAddress(statement.getConnection().getMetaData().getURL());

            boolean isQuery = true;
            boolean failed = false;

            // 尝试记录新的mysql连接
            MysqlCounterStorage mysqlCounterStorage = mysqlCounterStorageMap.computeIfAbsent(hostPort,
                                                                                             k -> new MysqlCounterStorage());

            if (MySqlTransformer.METHOD_EXECUTE_UPDATE.equals(methodName) ||
                MySqlTransformer.METHOD_EXECUTE_UPDATE_INTERNAL.equals(methodName)) {
                isQuery = false;
            } else if ((MySqlTransformer.METHOD_EXECUTE.equals(methodName) ||
                MySqlTransformer.METHOD_EXECUTE_INTERNAL.equals(methodName))) {
                Object result = afterJoinPoint.getResult();
                if (result != null && result instanceof Boolean && !(boolean) result) {
                    isQuery = false;
                }
            }

            if (null != afterJoinPoint.getException()) {
                failed = true;
            }

            mysqlCounterStorage.add(isQuery, failed, costTime);
        } catch (SQLException e) {
            log.error("unknown or unreachable connection intercepted by agent! this data may not been recorded!", e);
        } catch (URISyntaxException e) {
            log.error("db url parse failed!", e);
        }
    }

    @Override
    public boolean isEmpty() {
        return mysqlCounterStorageMap.isEmpty();
    }

    @Override
    public List<?> buildAndGetThriftEntities(int interval,
                                             String appName,
                                             String ipAddress,
                                             int port) {
        List<SqlInfoEntity> sqlInfoEntityList = new ArrayList<>();

        for (Map.Entry<String, MysqlCounterStorage> entry : mysqlCounterStorageMap.entrySet()) {
            SqlInfoEntity sqlInfoEntity = new SqlInfoEntity(appName,
                                                            ipAddress,
                                                            port,
                                                            System.currentTimeMillis(),
                                                            interval,
                                                            null,
                                                            new SqlPerformanceEntity(entry.getValue()
                                                                                         .getAndClearTotalCostTime(),
                                                                                     entry.getValue()
                                                                                         .getAndClearTotalFailureCount(),
                                                                                     entry.getValue()
                                                                                         .getAndClearTotalCount(),
                                                                                     entry.getValue()
                                                                                         .getAndClearTotalQueryCount(),
                                                                                     entry.getValue()
                                                                                         .getAndClearTotalUpdateCount(),
                                                                                     entry.getKey()));

            sqlInfoEntity.setDriverType(DRIVER_TYPE_MYSQL);

            sqlInfoEntity.setRequestByteSize(entry.getValue().getAndClearTotalBytesOut());
            sqlInfoEntity.setResponseByteSize(entry.getValue().getAndClearTotalBytesIn());
            sqlInfoEntityList.add(sqlInfoEntity);
        }

        return sqlInfoEntityList;
    }

    /**
     * 解析db地址, 去除不需要的部分
     *
     * @param rawUrl 数据源地址
     * @return 解析后地址
     * @throws URISyntaxException uriSyntaxException
     */
    private String parseDBAddress(String rawUrl) throws URISyntaxException {
        // 去掉jdbc:前缀, 会影响解析
        String originUrl = rawUrl.replaceFirst("jdbc:", "");

        URI uri = new URI(originUrl);

        return uri.getHost() + ":" + uri.getPort();
    }
}
