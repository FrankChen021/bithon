package com.sbss.bithon.agent.plugin.jdbc.druid.metric;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceStatValue;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metrics.IMetricProvider;
import com.sbss.bithon.agent.core.metrics.MetricProviderManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author frankchen
 */
public class DruidJdbcMetricProvider implements IMetricProvider {

    private static final String PROVIDER_NAME = "jdbc-druid-metrics";

    private static final DruidJdbcMetricProvider INSTANCE = new DruidJdbcMetricProvider();

    private DruidJdbcMetricProvider() {
        MetricProviderManager.getInstance().register(PROVIDER_NAME, this);
    }

    public static DruidJdbcMetricProvider getOrCreateInstance() {
        return INSTANCE;
    }

    public void updateMetrics(DruidDataSource dataSource,
                              DruidDataSourceStatValue statistic) {
        if (statistic == null) {
            return;
        }
        MonitoredSource source = MonitoredSourceManager.getInstance().getDataSource(dataSource);
        if (source == null) {
            return;
        }
        source.getJdbcMetric().activeCount.update(dataSource.getActiveCount());
        source.getJdbcMetric().createCount.update(statistic.getPhysicalConnectCount());
        source.getJdbcMetric().destroyCount.update(statistic.getPhysicalCloseCount());
        source.getJdbcMetric().createErrorCount.update(statistic.getPhysicalConnectErrorCount());
        source.getJdbcMetric().poolingPeak.add(statistic.getPoolingPeak());
        source.getJdbcMetric().activePeak.add(statistic.getActivePeak());
        source.getJdbcMetric().logicConnectionCount.update(statistic.getConnectCount());
        source.getJdbcMetric().logicCloseCount.update(statistic.getCloseCount());
        source.getJdbcMetric().executeCount.update(statistic.getExecuteCount());
        source.getJdbcMetric().commitCount.update(statistic.getCommitCount());
        source.getJdbcMetric().rollbackCount.update(statistic.getRollbackCount());
        source.getJdbcMetric().startTransactionCount.update(statistic.getStartTransactionCount());
    }

    @Override
    public boolean isEmpty() {
        return MonitoredSourceManager.getInstance().isEmpty();
    }

    @Override
    public List<Object> buildMessages(IMessageConverter messageConverter,
                                      AppInstance appInstance,
                                      int interval,
                                      long timestamp) {
        List<Object> jdbcMessages = new ArrayList<>();

        Collection<MonitoredSource> dataSources = MonitoredSourceManager.getInstance().getDataSources();
        dataSources.forEach((monitoredSource) -> {
            monitoredSource.getDataSource().getStatValueAndReset();

            jdbcMessages.add(messageConverter.from(appInstance,
                                                   timestamp,
                                                   interval,
                                                   monitoredSource.getJdbcMetric()));
        });

        return jdbcMessages;
    }
}
