package com.sbss.bithon.agent.core.metrics.redis;

import com.sbss.bithon.agent.core.metrics.Counter;

/**
 * @author frankchen
 */
public class RedisMetric {

    private final String hostAndPort;
    private final String command;

    private final Counter requestTime = new Counter();
    private final Counter responseTime = new Counter();
    private final Counter totalCount = new Counter();
    private final Counter exceptionCount = new Counter();
    private final Counter responseBytes = new Counter();
    private final Counter requestBytes = new Counter();

    public RedisMetric(String hostAndPort, String command) {
        this.hostAndPort = hostAndPort;
        this.command = command;
    }

    public void addRequest(long writeCostTime, int exceptionCount) {
        this.requestTime.update(writeCostTime);
        this.exceptionCount.update(exceptionCount);
        this.totalCount.incr();
    }

    public void addResponse(long readCostTime, int exceptionCount) {
        this.responseTime.update(readCostTime);
        this.exceptionCount.update(exceptionCount);
    }

    public long getRequestTime() {
        return requestTime.get();
    }

    public long getResponseTime() {
        return responseTime.get();
    }

    public long getTotalCount() {
        return totalCount.get();
    }

    public long getExceptionCount() {
        return exceptionCount.get();
    }

    public String getHostAndPort() {
        return hostAndPort;
    }

    public String getCommand() {
        return command;
    }

    public long getResponseBytes() {
        return responseBytes.get();
    }

    public long getRequestBytes() {
        return requestBytes.get();
    }

    public void addResponseBytes(int responseBytes) {
        this.responseBytes.update(responseBytes);
    }

    public void addRequestBytes(int requestBytes) {
        this.requestBytes.update(requestBytes);
    }
}
