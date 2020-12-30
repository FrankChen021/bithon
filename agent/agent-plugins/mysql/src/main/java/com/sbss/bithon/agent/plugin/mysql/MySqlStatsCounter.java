package com.sbss.bithon.agent.plugin.mysql;

import com.keruyun.commons.agent.collector.entity.JdbcConnectionEntity;
import com.keruyun.commons.agent.collector.entity.JdbcSqlStatEntity;
import com.sbss.bithon.agent.core.context.ContextHolder;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.dispatcher.config.IConfigSynchronizedListener;
import com.sbss.bithon.agent.dispatcher.metrics.counter.AgentCounterRepository;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;
import shaded.com.alibaba.druid.sql.visitor.ParameterizedOutputVisitorUtils;
import shaded.com.alibaba.druid.util.JdbcConstants;
import shaded.com.alibaba.fastjson.JSONObject;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;

/**
 * Description : mysql 执行计数器 <br>
 * Date: 17/10/31
 *
 * @author 马至远
 */
public class MySqlStatsCounter implements IAgentCounter, IConfigSynchronizedListener {
    private static final Logger log = LoggerFactory.getLogger(MySqlStatsCounter.class);

    private static final String MYSQL_COUNTER_NAME = "sql_stats";

    // private Map<String, SqlValueStats> druidSqlValueStatsMap = new
    // ConcurrentHashMap<>();

    private Map<String, Map<String, SqlValueStats>> sqlValueStatsMap = new ConcurrentHashMap<>();
    private long sqlTime = 1000;

    @Override
    public void onSync(JSONObject config) {
        Object val = config.get("sqlTime");
        if (val != null && val instanceof Number) {
            this.sqlTime = ((Number) val).intValue();
        }
    }

    private static class MySqlCounterHolder {
        static final MySqlStatsCounter INSTANCE = new MySqlStatsCounter();
    }

    static MySqlStatsCounter getInstance() {
        return MySqlCounterHolder.INSTANCE;
    }

    private MySqlStatsCounter() {

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
            Statement statement = (Statement) afterJoinPoint.getTarget();
            try {
                sqlStats(afterJoinPoint, costTime, statement.getConnection().getMetaData().getURL());
            } catch (SQLException e) {
                log.error("sql语句统计出错", e);
            }
        }
    }

    private void sqlStats(AfterJoinPoint afterJoinPoint,
                          long costTime,
                          String dataSourceUrl) {
        String sql = (String) ContextHolder.get("slowSql");
        ContextHolder.remove("slowSql");
        try {
            dataSourceUrl = parseDBAddress(dataSourceUrl);
        } catch (URISyntaxException e) {
            log.error("解析dataSourceURL错误", e);
        }
        costTime = costTime / 1000000;
        if (sql != null && costTime >= sqlTime) {
            sql = ParameterizedOutputVisitorUtils.parameterize(sql, JdbcConstants.MYSQL).replace("\n", "");
            Map<String, SqlValueStats> map = null;
            if ((map = sqlValueStatsMap.get(dataSourceUrl)) == null) {
                synchronized (this) {
                    if ((map = sqlValueStatsMap.get(dataSourceUrl)) == null) {
                        map = sqlValueStatsMap.putIfAbsent(dataSourceUrl, new ConcurrentHashMap<>());
                        if (map == null) {
                            map = sqlValueStatsMap.get(dataSourceUrl);
                        }
                    }
                }
            }
            SqlValueStats sqlValueStats = null;
            if ((sqlValueStats = map.get(sql)) == null) {
                synchronized (this) {
                    if ((sqlValueStats = map.get(sql)) == null) {
                        sqlValueStats = map.putIfAbsent(sql, new SqlValueStats());
                        if (sqlValueStats == null) {
                            sqlValueStats = map.get(sql);
                        }
                    }
                }
            }
            boolean failed = false;
            if (null != afterJoinPoint.getException()) {
                failed = true;
            }
            sqlValueStats.add(1, failed ? 1 : 0, costTime);
        }
    }

    @Override
    public boolean isEmpty() {
        return sqlValueStatsMap.isEmpty();
    }

    @Override
    public List<?> buildAndGetThriftEntities(int interval,
                                             String appName,
                                             String ipAddress,
                                             int port) {
        List<JdbcConnectionEntity> jdbcSqlStatEntityList = new ArrayList<>();
        sqlValueStatsMap.forEach((k,
                                  v) -> jdbcSqlStatEntityList.add(buildEntity(k,
                                                                              v,
                                                                              interval,
                                                                              appName,
                                                                              ipAddress,
                                                                              port)));
        return jdbcSqlStatEntityList;
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

    private class SqlValueStats {
        private AtomicLong executeCount = new AtomicLong(0);
        private AtomicLong executeErrorCount = new AtomicLong(0);
        private AtomicLong totalTime = new AtomicLong(0);
        private LongAccumulator lastTime = new LongAccumulator(Long::max, 0L);
        private LongAccumulator maxTimespan = new LongAccumulator(Long::max, 0L);
        private AtomicLong effectedRowCount = new AtomicLong();
        private AtomicLong fetchedRowCount = new AtomicLong();
        private LongAccumulator batchSizeMax = new LongAccumulator(Long::max, 0L);
        private AtomicLong batchSizeTotal = new AtomicLong();
        private LongAccumulator concurrentMax = new LongAccumulator(Long::max, 0L);

        SqlValueStats add(long executeNum,
                          long executeErrorNum,
                          long executeTime) {
            executeCount.addAndGet(executeNum);
            executeErrorCount.addAndGet(executeErrorNum);
            totalTime.addAndGet(executeTime);
            lastTime.accumulate(System.currentTimeMillis());
            maxTimespan.accumulate(executeTime);

            /*
             * effectedRowCount.addAndGet(appender.effectedRowCount.get());
             * fetchedRowCount.addAndGet(appender.fetchedRowCount.get());
             * batchSizeMax.accumulate(appender.batchSizeMax.get());
             * batchSizeTotal.addAndGet(appender.batchSizeTotal.get());
             * concurrentMax.accumulate(appender.concurrentMax.get());
             */

            return this;
        }
    }

    private JdbcConnectionEntity buildEntity(String addrAndPort,
                                             Map<String, SqlValueStats> map,
                                             int interval,
                                             String appName,
                                             String ipAddress,
                                             int port) {

        List<JdbcSqlStatEntity> jdbcSqlStatEntityList = new ArrayList<>();
        // map.forEach((k, v) -> System.out.println(k+"========="+v.executeCount));
        map.forEach((k,
                     v) -> jdbcSqlStatEntityList.add(new JdbcSqlStatEntity(k,
                                                                           v.executeCount.getAndSet(0),
                                                                           v.executeErrorCount.getAndSet(0),
                                                                           v.totalTime.getAndSet(0),
                                                                           v.lastTime.getThenReset(),
                                                                           v.maxTimespan.getThenReset(),
                                                                           v.effectedRowCount.getAndSet(0),
                                                                           v.fetchedRowCount.getAndSet(0),
                                                                           v.batchSizeMax.getThenReset(),
                                                                           v.batchSizeTotal.getAndSet(0),
                                                                           v.concurrentMax.getThenReset())));

        map.clear();
        String driverType = "mysql";
        JdbcConnectionEntity jdbcConnectionEntity = new JdbcConnectionEntity(appName,
                                                                             ipAddress,
                                                                             port,
                                                                             System.currentTimeMillis(),
                                                                             interval,
                                                                             null,
                                                                             0,
                                                                             0,
                                                                             addrAndPort);

        jdbcConnectionEntity.setDriverType(driverType);
        jdbcConnectionEntity.setSqlStats(jdbcSqlStatEntityList);

        return jdbcConnectionEntity;
    }
}
