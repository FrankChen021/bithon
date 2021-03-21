package com.sbss.bithon.agent.core.metric.mongo;

import com.sbss.bithon.agent.core.metric.Sum;
import com.sbss.bithon.agent.core.metric.Timer;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 11:35 下午
 */
public class MongoDbMetricSet {
    /**
     * host + port
     */
    String hostPort;

    /**
     * commands total costTime
     */
    Timer responseTime = new Timer();
    Sum callCount = new Sum();
    Sum failureCount = new Sum();
    Sum bytesIn = new Sum();
    Sum bytesOut = new Sum();

    public MongoDbMetricSet(String hostPort) {
        this.hostPort = hostPort;
    }

    public void add(long responseTime, int failureCount) {
        this.callCount.incr();
        this.responseTime.update(responseTime);
        this.failureCount.update(failureCount);
    }

    public void addBytesIn(int bytesIn) {
        this.bytesIn.update(bytesIn);
    }

    public void addBytesOut(int bytesOut) {
        this.bytesOut.update(bytesOut);
    }

    String getHostPort() {
        return hostPort;
    }

    long getAndClearCostTime() {
        return responseTime.getSum().get();
    }

    long getAndClearCommands() {
        return callCount.get();
    }

    long getAndClearFailureCount() {
        return failureCount.get();
    }

    long getAndClearBytesIn() {
        return bytesIn.get();
    }

    long getAndClearBytesOut() {
        return bytesOut.get();
    }
}
