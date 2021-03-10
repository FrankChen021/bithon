package com.sbss.bithon.agent.core.metrics.jdbc;

import com.sbss.bithon.agent.core.metrics.Counter;
import com.sbss.bithon.agent.core.metrics.Gauge;

/**
 * @author frankchen
 */
public class JdbcPoolMetric {
    // dimension
    private final String connectionString;
    private final String driverClass;

    // metrics
    public Counter activeCount = new Counter();
    public Counter createCount = new Counter();
    public Counter destroyCount = new Counter();
    public Gauge poolingPeak = new Gauge();
    public Gauge activePeak = new Gauge();
    public Counter logicConnectionCount = new Counter();
    public Counter logicCloseCount = new Counter();
    public Counter createErrorCount = new Counter();
    public Counter executeCount = new Counter();
    public Counter commitCount = new Counter();
    public Counter rollbackCount = new Counter();
    public Counter startTransactionCount = new Counter();

    public JdbcPoolMetric(String connectionString, String driverClass) {
        this.connectionString = connectionString;
        this.driverClass = driverClass;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getDriverClass() {
        return driverClass;
    }
}
