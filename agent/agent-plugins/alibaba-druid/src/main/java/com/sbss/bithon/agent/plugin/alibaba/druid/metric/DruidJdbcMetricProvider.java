package com.sbss.bithon.agent.plugin.alibaba.druid.metric;

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

    private static final String PROVIDER_NAME = "alibaba-druid-jdbc-metrics";

    private static final DruidJdbcMetricProvider INSTANCE = new DruidJdbcMetricProvider();

    public static DruidJdbcMetricProvider getOrCreateInstance() {
        return INSTANCE;
    }

    private DruidJdbcMetricProvider() {
        MetricProviderManager.getInstance().register(PROVIDER_NAME, this);
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
        source.getJdbcMetric().activeCount.add(dataSource.getActiveCount());
        source.getJdbcMetric().createCount.add(statistic.getPhysicalConnectCount());
        source.getJdbcMetric().destroyCount.add(statistic.getPhysicalCloseCount());
        source.getJdbcMetric().createErrorCount.add(statistic.getPhysicalConnectErrorCount());
        source.getJdbcMetric().poolingPeak.add(statistic.getPoolingPeak());
        source.getJdbcMetric().activePeak.add(statistic.getActivePeak());
        source.getJdbcMetric().logicConnectionCount.add(statistic.getConnectCount());
        source.getJdbcMetric().logicCloseCount.add(statistic.getCloseCount());
        source.getJdbcMetric().executeCount.add(statistic.getExecuteCount());
        source.getJdbcMetric().commitCount.add(statistic.getCommitCount());
        source.getJdbcMetric().rollbackCount.add(statistic.getRollbackCount());
        source.getJdbcMetric().startTransactionCount.add(statistic.getStartTransactionCount());
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
