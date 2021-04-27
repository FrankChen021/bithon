/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.mysql.metrics;

import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IMetricCollector;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlStatementCompositeMetric;
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
public class StatementMetricCollector implements IMetricCollector, IAgentSettingRefreshListener {
    static final StatementMetricCollector INSTANCE = new StatementMetricCollector();
    private static final Logger log = LoggerFactory.getLogger(StatementMetricCollector.class);
    private static final String MYSQL_COUNTER_NAME = "sql_stats";
    private final Map<String, Map<String, SqlStatementCompositeMetric>> metricMap = new ConcurrentHashMap<>();
    private long sqlTime = 1000;

    private StatementMetricCollector() {
        try {
            MetricCollectorManager.getInstance().register(MYSQL_COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("druid counter init failed due to ", e);
        }

        AgentSettingManager.getInstance().register(SettingRootNames.SQL, this);
    }

    static StatementMetricCollector getInstance() {
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
                         String connectionString) {
        long responseTime = aopContext.getCostTime();

        String sql = (String) InterceptorContext.get("sql");

        responseTime = responseTime / 1000000;
        if (sql == null || responseTime < sqlTime) {
            return;
        }

        sql = ParameterizedOutputVisitorUtils.parameterize(sql, JdbcConstants.MYSQL).replace("\n", "");
        metricMap.computeIfAbsent(connectionString, key -> new ConcurrentHashMap<>())
                 .computeIfAbsent(sql, sqlKey -> new SqlStatementCompositeMetric("mysql"))
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
        metricMap.forEach((dataSourceUrl, statementMetrics) -> {
            //TODO: clear map
            statementMetrics.forEach((sql, metric) -> {
                metric.setSql(sql);
                messages.add(messageConverter.from(timestamp, interval, metric));
            });
        });
        return messages;
    }
}
