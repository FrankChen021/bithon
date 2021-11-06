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

package org.bithon.agent.plugin.jdbc.druid.metric;

import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IMetricCollector;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.sql.SqlCompositeMetric;
import org.bithon.agent.plugin.jdbc.druid.DruidPlugin;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * collect SQL related metrics if there's no underlying metric collector
 * This is useful when underlying driver is not MYSQL(which has its own plugin)
 *
 * @author frankchen
 */
public class DruidSqlMetricCollector implements IMetricCollector {
    private static final Logger log = LoggerFactory.getLogger(DruidSqlMetricCollector.class);

    public void update(String methodName,
                       String connectionString,
                       AopContext aopContext,
                       long costTime) {
        MonitoredSource monitoredSource = MonitoredSourceManager.getInstance().getMonitoredDataSource(connectionString);
        if (monitoredSource == null) {
            return;
        }

        // check if metrics provider for this driver exists
        // TODO: DriverClass has to be consistent with MySqlPlugin's collector name
        if (MetricCollectorManager.getInstance().collectorExists(monitoredSource.getDriverClass())) {
            log.debug("Underlying Metric Provider Exists");
            return;
        }

        Boolean isQuery = null;
        if (DruidPlugin.METHOD_EXECUTE_UPDATE.equals(methodName)
            || DruidPlugin.METHOD_EXECUTE_BATCH.equals(methodName)) {
            isQuery = false;
        } else if (DruidPlugin.METHOD_EXECUTE.equals(methodName)) {
            /*
             * execute method return true if the first result is a ResultSet
             */
            isQuery = aopContext.getReturning() == null ? null : (boolean) aopContext.castReturningAs();
        } else if (DruidPlugin.METHOD_EXECUTE_QUERY.equals(methodName)) {
            isQuery = true;
        } else {
            //TODO: parse the SQL to check if it's a SELECT
            log.warn("unknown method intercepted by druid-sql-counter : {}", methodName);
        }

        monitoredSource.getSqlMetric().update(isQuery, aopContext.hasException(), costTime);
    }

    @Override
    public boolean isEmpty() {
        return MonitoredSourceManager.getInstance().isEmpty();
    }

    @Override
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {
        List<Object> messages = new ArrayList<>();
        for (MonitoredSource source : MonitoredSourceManager.getInstance().getMonitoredSources()) {
            SqlCompositeMetric metric = source.getSqlMetric();
            if (metric.peekTotalCount() > 0) {
                Object message = messageConverter.from(timestamp,
                                                       interval,
                                                       Collections.singletonList(source.getConnectionString()),
                                                       source.getSqlMetric());
                if (message != null) {
                    messages.add(message);
                }
            }
        }
        return messages;
    }
}
