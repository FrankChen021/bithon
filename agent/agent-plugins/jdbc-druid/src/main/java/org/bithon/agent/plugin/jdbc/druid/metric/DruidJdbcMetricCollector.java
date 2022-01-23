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

import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IntervalMetricCollector;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.jdbc.JdbcPoolMetrics;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author frankchen
 */
public class DruidJdbcMetricCollector extends IntervalMetricCollector<JdbcPoolMetrics> {

    private static final DruidJdbcMetricCollector INSTANCE = new DruidJdbcMetricCollector();

    private DruidJdbcMetricCollector() {
        super("jdbc-pool-metrics",
              Arrays.asList("connectionString", "driverClass", "objectId"),
              JdbcPoolMetrics.class,
              null,
              false);

        MetricCollectorManager.getInstance().register("jdbc-druid-metrics", this);
    }

    public static DruidJdbcMetricCollector getOrCreateInstance() {
        return INSTANCE;
    }

    @Override
    public Object collect(IMessageConverter messageConverter, int interval, long timestamp) {
        Collection<MonitoredSource> dataSources = MonitoredSourceManager.getInstance().getMonitoredSources();
        dataSources.forEach((monitoredSource) -> monitoredSource.getDataSource().getStatValueAndReset());

        return super.collect(messageConverter, interval, timestamp);
    }
}
