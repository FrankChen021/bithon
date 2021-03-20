package com.sbss.bithon.agent.plugin.jdbc.druid.metric;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.IMetricCollector;
import com.sbss.bithon.agent.core.metric.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.sql.SqlMetric;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.plugin.jdbc.druid.DruidPlugin;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frankchen
 */
public class DruidSqlMetricCollector implements IMetricCollector {
    static final DruidSqlMetricCollector INSTANCE = new DruidSqlMetricCollector();
    private static final Logger log = LoggerFactory.getLogger(DruidSqlMetricCollector.class);
    private static final String METRICS_NAME = "sql-druid";

    private final MonitoredSourceManager monitoredSourceManager;

    private DruidSqlMetricCollector() {
        monitoredSourceManager = MonitoredSourceManager.getInstance();

        MetricCollectorManager.getInstance().register(METRICS_NAME, this);
    }

    public static DruidSqlMetricCollector getInstance() {
        return INSTANCE;
    }

    public void update(String methodName,
                       String dataSourceUri,
                       AopContext aopContext,
                       long costTime) {
        MonitoredSource monitoredSource = monitoredSourceManager.getMonitoredDataSource(dataSourceUri);
        if (monitoredSource == null) {
            return;
        }

        // check if metrics provider for this driver exists
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
            monitoredSource.getSqlMetric().add(isQuery, aopContext.hasException(), costTime);
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
            SqlMetric metric = source.getSqlMetric();
            if (metric.peekTotalCount() > 0) {
                Object message = messageConverter.from(timestamp, interval, source.getSqlMetric());
                if (message != null) {
                    messages.add(message);
                }
            }
        }
        return messages;
    }
}
