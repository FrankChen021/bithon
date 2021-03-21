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
    String endpoint;

    Timer responseTime = new Timer();
    Sum callCount = new Sum();
    Sum exceptionCount = new Sum();
    Sum bytesIn = new Sum();
    Sum bytesOut = new Sum();

    public MongoDbMetricSet(String endpoint) {
        this.endpoint = endpoint;
    }

    public void add(long responseTime, int failureCount) {
        this.callCount.incr();
        this.responseTime.update(responseTime);
        this.exceptionCount.update(failureCount);
    }

    public void addBytesIn(int bytesIn) {
        this.bytesIn.update(bytesIn);
    }

    public void addBytesOut(int bytesOut) {
        this.bytesOut.update(bytesOut);
    }

    String getEndpoint() {
        return endpoint;
    }

    long getAndClearCostTime() {
        return responseTime.getSum().get();
    }

    long getAndClearCommands() {
        return callCount.get();
    }

    long getAndClearFailureCount() {
        return exceptionCount.get();
    }

    long getAndClearBytesIn() {
        return bytesIn.get();
    }

    long getAndClearBytesOut() {
        return bytesOut.get();
    }
}
