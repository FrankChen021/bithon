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

package org.bithon.agent.plugin.mysql.metrics;

import org.bithon.agent.bootstrap.aop.context.AopContext;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.observability.dispatcher.IMessageConverter;
import org.bithon.agent.observability.metric.collector.IMetricCollector;
import org.bithon.agent.observability.metric.collector.MetricCollectorManager;
import org.bithon.agent.observability.metric.domain.sql.SQLStatementMetrics;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.shaded.com.alibaba.druid.sql.visitor.ParameterizedOutputVisitorUtils;
import org.bithon.shaded.com.alibaba.druid.util.JdbcConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author frankchen
 */
public class StatementMetricCollector implements IMetricCollector {
    private static final ILogAdaptor log = LoggerFactory.getLogger(StatementMetricCollector.class);
    private static final String MYSQL_COUNTER_NAME = "sql_stats";
    private static volatile StatementMetricCollector INSTANCE;
    private final Map<String, Map<String, SQLStatementMetrics>> metricMap = new ConcurrentHashMap<>();

    // TODO: Turn into dynamic configuration
    private long sqlTime = 1000;

    private StatementMetricCollector() {
        try {
            MetricCollectorManager.getInstance().register(MYSQL_COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("druid counter init failed due to ", e);
        }
    }

    static StatementMetricCollector getInstance() {
        if (INSTANCE == null) {
            synchronized (StatementMetricCollector.class) {
                if (INSTANCE == null) {
                    INSTANCE = new StatementMetricCollector();
                }
            }
        }
        return INSTANCE;
    }

    public void sqlStats(AopContext aopContext,
                         String connectionString) {
        long responseTime = aopContext.getExecutionTime();

        String sql = (String) InterceptorContext.get("sql");

        responseTime = responseTime / 1000000;
        if (sql == null || responseTime < sqlTime) {
            return;
        }

        sql = ParameterizedOutputVisitorUtils.parameterize(sql, JdbcConstants.MYSQL).replace("\n", "");
        metricMap.computeIfAbsent(connectionString, key -> new ConcurrentHashMap<>())
                 .computeIfAbsent(sql, sqlKey -> new SQLStatementMetrics("mysql"))
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
