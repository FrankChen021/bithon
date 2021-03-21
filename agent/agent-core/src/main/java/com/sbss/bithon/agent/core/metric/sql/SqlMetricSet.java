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
    private final String connectionString;

    // metric
    private final Timer responseTime = new Timer();
    private final Sum callCount = new Sum();
    private final Sum errorCount = new Sum();
    private final Sum queryCount = new Sum();
    private final Sum updateCount = new Sum();
    private final Sum totalBytesIn = new Sum();
    private final Sum totalBytesOut = new Sum();

    public SqlMetricSet(String connectionString, String driverType) {
        this.connectionString = connectionString;
        this.driverType = driverType;
    }

    public void add(boolean isQuery, boolean failed, long responseTime) {
        this.responseTime.update(responseTime);
        if (isQuery) {
            this.queryCount.incr();
        } else {
            this.updateCount.incr();
        }

        if (failed) {
            this.errorCount.incr();
        }

        this.callCount.incr();
    }

    public long peekTotalCount() {
        return callCount.peek();
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

    public String getConnectionString() {
        return connectionString;
    }

    public Timer getResponseTime() {
        return responseTime;
    }

    public Sum getCallCount() {
        return callCount;
    }

    public Sum getErrorCount() {
        return errorCount;
    }

    public Sum getQueryCount() {
        return queryCount;
    }

    public Sum getUpdateCount() {
        return updateCount;
    }

    public Sum getTotalBytesIn() {
        return totalBytesIn;
    }

    public Sum getTotalBytesOut() {
        return totalBytesOut;
    }
}
