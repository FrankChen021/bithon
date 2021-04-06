package com.sbss.bithon.agent.plugin.mysql8;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IMetricCollector;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlStatementCompositeMetric;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.core.setting.AgentSettingManager;
import com.sbss.bithon.agent.core.setting.IAgentSettingRefreshListener;
import com.sbss.bithon.agent.core.setting.SettingRootNames;
import com.sbss.bithon.agent.core.utils.MiscUtils;
import shaded.com.alibaba.druid.sql.visitor.ParameterizedOutputVisitorUtils;
import shaded.com.alibaba.druid.util.JdbcConstants;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slow SQL
 *
 * @author frankchen
 */
public class SqlStatementMetricCollector implements IMetricCollector, IAgentSettingRefreshListener {
    static final SqlStatementMetricCollector INSTANCE = new SqlStatementMetricCollector();
    private static final Logger log = LoggerFactory.getLogger(SqlStatementMetricCollector.class);
    private static final String MYSQL_COUNTER_NAME = "mysql8_sql_stats";
    private final Map<String, Map<String, SqlStatementCompositeMetric>> metricMap = new ConcurrentHashMap<>();
    private int sqlTimeThreshold = 1000;

    private SqlStatementMetricCollector() {
        try {
            MetricCollectorManager.getInstance().register(MYSQL_COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("druid counter init failed due to ", e);
        }

        AgentSettingManager.getInstance().register(SettingRootNames.SQL, this);
    }

    static SqlStatementMetricCollector getInstance() {
        return INSTANCE;
    }

    public void update(AopContext aopContext) {
        long costTime = aopContext.getCostTime();

        if (!(aopContext.getTarget() instanceof Statement)) {
            return;
        }

        Statement statement = (Statement) aopContext.getTarget();
        try {
            if (statement.isClosed()) {
                log.warn("Connection has been shutdown");
                return;
            }
            slowSqlStats(aopContext,
                         costTime,
                         MiscUtils.cleanupConnectionString(statement.getConnection().getMetaData().getURL()));
        } catch (SQLException e) {
            log.warn("error when counting SQL statement", e);
        }
    }

    /**
     * TODO: use EVENT instead of metrics to report SLOW SQL
     */
    private void slowSqlStats(AopContext aopContext, long responseTime, String connectionString) {
        String sql = (String) InterceptorContext.get("sql");

        responseTime = responseTime / 1000000;
        if (sql == null || responseTime < sqlTimeThreshold) {
            return;
        }

        sql = ParameterizedOutputVisitorUtils.parameterize(sql, JdbcConstants.MYSQL).replace("\n", "");
        metricMap.computeIfAbsent(connectionString,
                                  key -> new ConcurrentHashMap<>())
                 .computeIfAbsent(sql, key -> new SqlStatementCompositeMetric("mysql"))
                 .add(1, aopContext.hasException() ? 1 : 0, responseTime);
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
        metricMap.forEach((dataSourceUrl, statementCounters) -> {
            statementCounters.forEach((sql, metric) -> {
                //TODO: clear map
                metric.setSql(sql);
                messages.add(messageConverter.from(timestamp,
                                                   interval,
                                                   metric));
            });
        });
        return messages;
    }

    @Override
    public void onRefresh(Map<String, Object> config) {
        Object val = config.get("sqlTime");
        if (val instanceof Number) {
            this.sqlTimeThreshold = ((Number) val).intValue();
        }
    }
}
