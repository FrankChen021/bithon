package com.sbss.bithon.agent.core.metrics.sql;

import com.sbss.bithon.agent.core.metrics.Counter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 11:18 下午
 */
public class SqlMetric {
    // dimension
    private final String driverType;
    private final String hostAndPort;

    // metric
    private final Counter totalCostTime = new Counter();
    private final Counter totalFailureCount = new Counter();
    private final Counter totalCount = new Counter();
    private final Counter totalQueryCount = new Counter();
    private final Counter totalUpdateCount = new Counter();
    private final Counter totalBytesIn = new Counter();
    private final Counter totalBytesOut = new Counter();

    public SqlMetric(String hostAndPort, String driverType) {
        this.hostAndPort = hostAndPort;
        this.driverType = driverType;
    }

    public void add(boolean isQuery, boolean failed, long costTime) {
        this.totalCostTime.update(costTime);
        if (isQuery) {
            this.totalQueryCount.incr();
        } else {
            this.totalUpdateCount.incr();
        }

        if (failed) {
            this.totalFailureCount.incr();
        }

        this.totalCount.incr();
    }

    public long getAndClearTotalCostTime() {
        return totalCostTime.get();
    }

    public long getAndClearTotalFailureCount() {
        return totalFailureCount.get();
    }

    public long peekTotalCount() {
        return totalCount.peek();
    }

    public long getAndClearTotalCount() {
        return totalCount.get();
    }

    public long getAndClearTotalQueryCount() {
        return totalQueryCount.get();
    }

    public long getAndClearTotalUpdateCount() {
        return totalUpdateCount.get();
    }

    public void addBytesIn(int bytesIn) {
        this.totalBytesIn.update(bytesIn);
    }

    public void addBytesOut(int bytesOut) {
        this.totalBytesOut.update(bytesOut);
    }

    public String getDriverType() {
        return driverType;
    }
}
