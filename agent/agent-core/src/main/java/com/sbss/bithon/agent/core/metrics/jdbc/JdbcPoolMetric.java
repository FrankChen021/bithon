package com.sbss.bithon.agent.core.metrics.jdbc;

import com.sbss.bithon.agent.core.metrics.Sum;
import com.sbss.bithon.agent.core.metrics.Gauge;

/**
 * @author frankchen
 */
public class JdbcPoolMetric {
    // dimension
    private final String connectionString;
    private final String driverClass;

    // metrics
    public Sum activeCount = new Sum();
    public Sum createCount = new Sum();
    public Sum destroyCount = new Sum();
    public Gauge poolingPeak = new Gauge();
    public Gauge activePeak = new Gauge();
    public Sum logicConnectionCount = new Sum();
    public Sum logicCloseCount = new Sum();
    public Sum createErrorCount = new Sum();
    public Sum executeCount = new Sum();
    public Sum commitCount = new Sum();
    public Sum rollbackCount = new Sum();
    public Sum startTransactionCount = new Sum();

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
