package com.sbss.bithon.agent.core.metric.domain.http;

import com.sbss.bithon.agent.core.metric.model.ICompositeMetric;
import com.sbss.bithon.agent.core.metric.model.Timer;
import com.sbss.bithon.agent.core.metric.model.Sum;

/**
 * @author frankchen
 */
public class HttpClientCompositeMetric implements ICompositeMetric {
    /**
     * total cost time in NANO second
     */
    private final Timer responseTime = new Timer();

    /**
     * count of all status code between 400(inclusive) and 500(exclusive)
     */
    private final Sum count4xx = new Sum();

    /**
     * count of all status code larger than 500(inclusive)
     */
    private final Sum count5xx = new Sum();
    private final Sum countException = new Sum();
    private final Sum requestCount = new Sum();
    private final Sum requestBytes = new Sum();
    private final Sum responseBytes = new Sum();

    public void add(long responseTime, int count4xx, int count5xx) {
        this.responseTime.update(responseTime);
        this.count4xx.update(count4xx);
        this.count5xx.update(count5xx);
        this.requestCount.incr();
    }

    public void addException(long responseTime, int exceptionCount) {
        this.responseTime.update(responseTime);
        this.countException.update(exceptionCount);
        this.requestCount.incr();
    }

    public void addByteSize(long requestByteSize, long responseByteSize) {
        this.requestBytes.update(requestByteSize);
        this.responseBytes.update(responseByteSize);
    }

    public Timer getResponseTime() {
        return responseTime;
    }

    public long getCount4xx() {
        return count4xx.get();
    }

    public long getCount5xx() {
        return count5xx.get();
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    public long getRequestBytes() {
        return requestBytes.get();
    }

    public long getResponseBytes() {
        return responseBytes.get();
    }

    public long getExceptionCount() {
        return countException.get();
    }
}
