package com.sbss.bithon.agent.plugin.jdbc.druid.metric;

import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IMetricCollector;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlCompositeMetric;
import com.sbss.bithon.agent.plugin.jdbc.druid.DruidPlugin;
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
        } else if (DruidPlugin.METHOD_EXECUTE.equals(methodName) && !(boolean) aopContext.castReturningAs()) {
            isQuery = false;
        } else if (DruidPlugin.METHOD_EXECUTE_QUERY.equals(methodName) || (boolean) aopContext.castReturningAs()) {
            isQuery = true;
        } else {
            log.debug("unknown method intercepted by druid-sql-counter : {}", methodName);
        }

        if (isQuery != null) {
            monitoredSource.getSqlMetric().update(isQuery, aopContext.hasException(), costTime);
        }
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
