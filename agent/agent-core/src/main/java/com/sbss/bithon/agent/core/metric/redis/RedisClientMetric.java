package com.sbss.bithon.agent.core.metric.redis;

import com.sbss.bithon.agent.core.metric.Timer;
import com.sbss.bithon.agent.core.metric.Sum;

/**
 * @author frankchen
 */
public class RedisClientMetric {

    private final String endpoint;
    private final String command;

    private final Timer requestTime = new Timer();
    private final Timer responseTime = new Timer();
    private final Sum callCount = new Sum();
    private final Sum exceptionCount = new Sum();
    private final Sum responseBytes = new Sum();
    private final Sum requestBytes = new Sum();

    public RedisClientMetric(String endpoint, String command) {
        this.endpoint = endpoint;
        this.command = command;
    }

    public void addRequest(long writeCostTime, int exceptionCount) {
        this.requestTime.update(writeCostTime);
        this.exceptionCount.update(exceptionCount);
        this.callCount.incr();
    }

    public void addResponse(long readCostTime, int exceptionCount) {
        this.responseTime.update(readCostTime);
        this.exceptionCount.update(exceptionCount);
    }

    public Timer getRequestTime() {
        return requestTime;
    }

    public Timer getResponseTime() {
        return responseTime;
    }

    public long getCallCount() {
        return callCount.get();
    }

    public long getExceptionCount() {
        return exceptionCount.get();
    }

    public String getEndpoint() {
        return endpoint;
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
