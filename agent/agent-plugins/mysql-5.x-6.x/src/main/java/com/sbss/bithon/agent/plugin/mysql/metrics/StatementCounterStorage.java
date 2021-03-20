package com.sbss.bithon.agent.plugin.mysql.metrics;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.IMetricCollector;
import com.sbss.bithon.agent.core.metric.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.sql.SqlStatementMetric;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.setting.AgentSettingManager;
import com.sbss.bithon.agent.core.setting.IAgentSettingRefreshListener;
import com.sbss.bithon.agent.core.setting.SettingRootNames;
import shaded.com.alibaba.druid.sql.visitor.ParameterizedOutputVisitorUtils;
import shaded.com.alibaba.druid.util.JdbcConstants;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author frankchen
 */
public class StatementCounterStorage implements IMetricCollector, IAgentSettingRefreshListener {
    static final StatementCounterStorage INSTANCE = new StatementCounterStorage();
    private static final Logger log = LoggerFactory.getLogger(StatementCounterStorage.class);
    private static final String MYSQL_COUNTER_NAME = "sql_stats";
    private final Map<String, Map<String, SqlStatementMetric>> metricMap = new ConcurrentHashMap<>();
    private long sqlTime = 1000;

    private StatementCounterStorage() {
        try {
            MetricCollectorManager.getInstance().register(MYSQL_COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("druid counter init failed due to ", e);
        }

        AgentSettingManager.getInstance().register(SettingRootNames.SQL, this);
    }

    static StatementCounterStorage getInstance() {
        return INSTANCE;
    }

    @Override
    public void onRefresh(Map<String, Object> config) {
        Object val = config.get("sqlTime");
        if (val instanceof Number) {
            this.sqlTime = ((Number) val).intValue();
        }
    }

    public void sqlStats(AopContext aopContext,
                         String hostAndPort) {
        long costTime = aopContext.getCostTime();

        String sql = (String) InterceptorContext.get("sql");

        costTime = costTime / 1000000;
        if (sql == null || costTime < sqlTime) {
            return;
        }

        sql = ParameterizedOutputVisitorUtils.parameterize(sql, JdbcConstants.MYSQL).replace("\n", "");
        Map<String, SqlStatementMetric> statementCounters;
        if ((statementCounters = metricMap.get(hostAndPort)) == null) {
            synchronized (this) {
                if ((statementCounters = metricMap.get(hostAndPort)) == null) {
                    statementCounters = metricMap.putIfAbsent(hostAndPort, new ConcurrentHashMap<>());
                    if (statementCounters == null) {
                        statementCounters = metricMap.get(hostAndPort);
                    }
                }
            }
        }
        SqlStatementMetric statementCounter;
        if ((statementCounter = statementCounters.get(sql)) == null) {
            synchronized (this) {
                if ((statementCounter = statementCounters.get(sql)) == null) {
                    statementCounter = statementCounters.putIfAbsent(sql, new SqlStatementMetric("mysql", sql));
                    if (statementCounter == null) {
                        statementCounter = statementCounters.get(sql);
                    }
                }
            }
        }
        boolean hasException = false;
        if (null != aopContext.getException()) {
            hasException = true;
        }
        statementCounter.add(1, hasException ? 1 : 0, costTime);
    }

    @Override
    public boolean isEmpty() {
        return metricMap.isEmpty();
    }

    @Override
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {
        List<Object> messages = new ArrayList<>();
        metricMap.forEach((dataSourceUrl, statementMetrics) -> {
            statementMetrics.forEach((sql, counter) -> messages.add(messageConverter.from(timestamp, interval, counter)));
        });
        return messages;
    }
}
