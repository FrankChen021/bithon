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

package com.sbss.bithon.agent.plugin.jdbc.druid.metric;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceStatValue;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IMetricCollector;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author frankchen
 */
public class DruidJdbcMetricCollector implements IMetricCollector {

    private static final DruidJdbcMetricCollector INSTANCE = new DruidJdbcMetricCollector();

    private DruidJdbcMetricCollector() {
        MetricCollectorManager.getInstance().register("jdbc-druid-metrics", this);
    }

    public static DruidJdbcMetricCollector getOrCreateInstance() {
        return INSTANCE;
    }

    public void updateMetrics(DruidDataSource dataSource,
                              DruidDataSourceStatValue statistic) {
        if (statistic == null) {
            return;
        }
        MonitoredSource source = MonitoredSourceManager.getInstance().getMonitoredDataSource(dataSource);
        if (source == null) {
            return;
        }

        source.getJdbcMetric().activeCount.update(dataSource.getActiveCount());
        source.getJdbcMetric().createCount.update(statistic.getPhysicalConnectCount());
        source.getJdbcMetric().destroyCount.update(statistic.getPhysicalCloseCount());
        source.getJdbcMetric().createErrorCount.update(statistic.getPhysicalConnectErrorCount());
        source.getJdbcMetric().poolingPeak.update(statistic.getPoolingPeak());
        source.getJdbcMetric().poolingCount.update(statistic.getPoolingCount());
        source.getJdbcMetric().activePeak.update(statistic.getActivePeak());
        source.getJdbcMetric().logicConnectionCount.update(statistic.getConnectCount());
        source.getJdbcMetric().logicCloseCount.update(statistic.getCloseCount());
        source.getJdbcMetric().executeCount.update(statistic.getExecuteCount());
        source.getJdbcMetric().commitCount.update(statistic.getCommitCount());
        source.getJdbcMetric().rollbackCount.update(statistic.getRollbackCount());
        source.getJdbcMetric().startTransactionCount.update(statistic.getStartTransactionCount());
        source.getJdbcMetric().waitThreadCount.update(statistic.getWaitThreadCount());
    }

    @Override
    public boolean isEmpty() {
        return MonitoredSourceManager.getInstance().isEmpty();
    }

    @Override
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {
        List<Object> jdbcMessages = new ArrayList<>();

        Collection<MonitoredSource> dataSources = MonitoredSourceManager.getInstance().getMonitoredSources();
        dataSources.forEach((monitoredSource) -> {
            monitoredSource.getDataSource().getStatValueAndReset();

            jdbcMessages.add(messageConverter.from(timestamp,
                                                   interval,
                                                   monitoredSource.getJdbcMetric()));
        });

        return jdbcMessages;
    }
}
