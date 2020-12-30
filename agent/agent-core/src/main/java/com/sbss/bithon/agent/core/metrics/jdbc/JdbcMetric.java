package com.sbss.bithon.agent.core.metrics.jdbc;

import com.sbss.bithon.agent.core.metrics.Counter;
import com.sbss.bithon.agent.core.metrics.Gauge;

/**
 * @author frankchen
 */
public class JdbcMetric {
    // dimension
    private final String uri;
    private final String driverType;

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

    public JdbcMetric(String uri, String driverType) {
        this.uri = uri;
        this.driverType = driverType;
    }

    public String getUri() {
        return uri;
    }

    public String getDriverType() {
        return driverType;
    }
}
