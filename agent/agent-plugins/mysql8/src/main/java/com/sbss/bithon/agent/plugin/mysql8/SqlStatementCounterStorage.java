package com.sbss.bithon.agent.plugin.mysql8;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.setting.AgentSettingManager;
import com.sbss.bithon.agent.core.setting.IAgentSettingRefreshListener;
import com.sbss.bithon.agent.core.setting.SettingRootNames;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.metrics.IMetricProvider;
import com.sbss.bithon.agent.core.metrics.sql.SqlStatementMetric;
import shaded.com.alibaba.druid.sql.visitor.ParameterizedOutputVisitorUtils;
import shaded.com.alibaba.druid.util.JdbcConstants;
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

/**
 * Slow SQL
 *
 * @author frankchen
 */
public class SqlStatementCounterStorage implements IMetricProvider, IAgentSettingRefreshListener {
    private static final Logger log = LoggerFactory.getLogger(SqlStatementCounterStorage.class);

    private static final String MYSQL_COUNTER_NAME = "mysql8_sql_stats";

    private final Map<String, Map<String, SqlStatementMetric>> counters = new ConcurrentHashMap<>();
    private int sqlTimeThreshold = 1000;

    static final SqlStatementCounterStorage INSTANCE = new SqlStatementCounterStorage();

    static SqlStatementCounterStorage getInstance() {
        return INSTANCE;
    }

    private SqlStatementCounterStorage() {
        try {
            MetricProviderManager.getInstance().register(MYSQL_COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("druid counter init failed due to ", e);
        }

        AgentSettingManager.getInstance().register(SettingRootNames.SQL, this);
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
                         statement.getConnection().getMetaData().getURL());
        } catch (SQLException e) {
            log.warn("error when counting SQL statement", e);
        }
    }

    private void slowSqlStats(AopContext aopContext, long costTime, String dataSourceUrl) {
        String sql = (String) InterceptorContext.get("sql");
        try {
            dataSourceUrl = parseDBAddress(dataSourceUrl);
        } catch (URISyntaxException e) {
            log.error("解析dataSourceURL错误", e);
        }
        costTime = costTime / 1000000;
        if (sql == null || costTime < sqlTimeThreshold) {
            return;
        }

        sql = ParameterizedOutputVisitorUtils.parameterize(sql, JdbcConstants.MYSQL).replace("\n", "");
        Map<String, SqlStatementMetric> map;
        if ((map = counters.get(dataSourceUrl)) == null) {
            synchronized (this) {
                if ((map = counters.get(dataSourceUrl)) == null) {
                    map = counters.putIfAbsent(dataSourceUrl, new ConcurrentHashMap<>());
                    if (map == null) {
                        map = counters.get(dataSourceUrl);
                    }
                }
            }
        }

        SqlStatementMetric counter;
        if ((counter = map.get(sql)) == null) {
            synchronized (this) {
                if ((counter = map.get(sql)) == null) {
                    counter = map.putIfAbsent(sql, new SqlStatementMetric("mysql", sql));
                    if (counter == null) {
                        counter = map.get(sql);
                    }
                }
            }
        }
        boolean hasException = false;
        if (null != aopContext.getException()) {
            hasException = true;
        }
        counter.add(1, hasException ? 1 : 0, costTime);
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
        List<Object> messages = new ArrayList<>();
        counters.forEach((dataSourceUrl, statementCounters) -> {
            statementCounters.forEach((sql, counter) -> messages.add(messageConverter.from(counter)));
        });
        return messages;
    }

    /**
     *
     */
    private String parseDBAddress(String rawUrl) throws URISyntaxException {
        String originUrl = rawUrl.replaceFirst("jdbc:", "");
        URI uri = new URI(originUrl);
        return uri.getHost() + ":" + uri.getPort();
    }

    @Override
    public void onRefresh(Map<String, Object> config) {
        Object val = config.get("sqlTime");
        if (val instanceof Number) {
            this.sqlTimeThreshold = ((Number) val).intValue();
        }
    }
}
