/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.agent.plugin.mysql8;

import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.controller.config.DynamicConfigurationManager;
import org.bithon.agent.controller.config.IAgentSettingRefreshListener;
import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IMetricCollector;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.sql.SQLStatementMetrics;
import org.bithon.agent.core.utils.MiscUtils;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.shaded.com.alibaba.druid.sql.visitor.ParameterizedOutputVisitorUtils;
import org.bithon.shaded.com.alibaba.druid.util.JdbcConstants;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;

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
    private static final ILogAdaptor log = LoggerFactory.getLogger(SqlStatementMetricCollector.class);
    private static final String MYSQL_COUNTER_NAME = "mysql8_sql_stats";
    private static SqlStatementMetricCollector INSTANCE;
    private final Map<String, Map<String, SQLStatementMetrics>> metricMap = new ConcurrentHashMap<>();
    private int sqlTimeThreshold = 1000;

    private SqlStatementMetricCollector() {
        try {
            MetricCollectorManager.getInstance().register(MYSQL_COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("druid counter init failed due to ", e);
        }

        DynamicConfigurationManager.getInstance().register("sql", this);
    }

    static SqlStatementMetricCollector getInstance() {
        if (INSTANCE == null) {
            synchronized (SqlStatementMetricCollector.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SqlStatementMetricCollector();
                }
            }
        }
        return INSTANCE;
    }

    public void update(AopContext aopContext) {
        long costTime = aopContext.getExecutionTime();

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
                 .computeIfAbsent(sql, key -> new SQLStatementMetrics("mysql"))
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
    public void onRefresh(ObjectMapper om, JsonNode configNode) {
        JsonNode val = configNode.get("sqlTime");
        if (val.isNumber()) {
            this.sqlTimeThreshold = val.asInt();
        }
    }
}
