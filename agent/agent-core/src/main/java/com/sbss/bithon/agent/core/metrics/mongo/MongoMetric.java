package com.sbss.bithon.agent.core.metrics.mongo;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 11:35 下午
 */
public class MongoMetric {
    /**
     * host + port
     */
    String hostPort;
    /**
     * commands total costTime
     */
    AtomicLong costTime = new AtomicLong(0);
    /**
     * commands count
     */
    AtomicInteger commands = new AtomicInteger(0);
    /**
     * commands failure count
     */
    AtomicInteger failureCount = new AtomicInteger(0);
    /**
     * bytes in
     */
    AtomicLong bytesIn = new AtomicLong(0);
    /**
     * bytes out
     */
    AtomicLong bytesOut = new AtomicLong(0);

    public MongoMetric(String hostPort) {
        this.hostPort = hostPort;
    }

    public void add(long costTime, int failureCount) {
        this.commands.incrementAndGet();
        this.costTime.addAndGet(costTime);
        this.failureCount.addAndGet(failureCount);
    }

    public void addBytesIn(int bytesIn) {
        this.bytesIn.addAndGet(bytesIn);
    }

    public void addBytesOut(int bytesOut) {
        this.bytesOut.addAndGet(bytesOut);
    }

    String getHostPort() {
        return hostPort;
    }

    long getAndClearCostTime() {
        return costTime.getAndSet(0);
    }

    int getAndClearCommands() {
        return commands.getAndSet(0);
    }

    int getAndClearFailureCount() {
        return failureCount.getAndSet(0);
    }

    long getAndClearBytesIn() {
        return bytesIn.getAndSet(0);
    }

    long getAndClearBytesOut() {
        return bytesOut.getAndSet(0);
    }
}
