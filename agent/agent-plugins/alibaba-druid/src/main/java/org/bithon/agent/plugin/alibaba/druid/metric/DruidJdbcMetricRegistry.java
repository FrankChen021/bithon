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

package org.bithon.agent.plugin.alibaba.druid.metric;

import org.bithon.agent.observability.metric.collector.MetricRegistry;
import org.bithon.agent.observability.metric.collector.MetricRegistryFactory;
import org.bithon.agent.observability.metric.domain.jdbc.JdbcPoolMetrics;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author frankchen
 */
public class DruidJdbcMetricRegistry extends MetricRegistry<JdbcPoolMetrics> {

    private DruidJdbcMetricRegistry() {
        super("jdbc-pool-metrics",
              Arrays.asList("connectionString", "driverClass", "objectId"),
              JdbcPoolMetrics.class,
              null,
              false);
    }

    public static DruidJdbcMetricRegistry get() {
        return MetricRegistryFactory.getOrCreateRegistry("jdbc-pool-metrics", DruidJdbcMetricRegistry::new);
    }

    @Override
    protected void onCollect() {
        // update the metrics
        Collection<MonitoredSource> dataSources = MonitoredSourceManager.getInstance().getMonitoredSources();
        dataSources.forEach((monitoredSource) -> monitoredSource.getDataSource().getStatValueAndReset());
    }
}
