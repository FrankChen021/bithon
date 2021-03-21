package com.sbss.bithon.agent.core.metric.sql;

import com.sbss.bithon.agent.core.metric.Timer;
import com.sbss.bithon.agent.core.metric.Sum;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 11:18 下午
 */
public class SqlMetricSet {
    // dimension
    private final String driverType;
    private final String hostAndPort;

    // metric
    private final Timer responseTime = new Timer();
    private final Sum totalFailureCount = new Sum();
    private final Sum totalCount = new Sum();
    private final Sum totalQueryCount = new Sum();
    private final Sum totalUpdateCount = new Sum();
    private final Sum totalBytesIn = new Sum();
    private final Sum totalBytesOut = new Sum();

    public SqlMetricSet(String hostAndPort, String driverType) {
        this.hostAndPort = hostAndPort;
        this.driverType = driverType;
    }

    public void add(boolean isQuery, boolean failed, long responseTime) {
        this.responseTime.update(responseTime);
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

    public Timer getResponseTime() {
        return responseTime;
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
