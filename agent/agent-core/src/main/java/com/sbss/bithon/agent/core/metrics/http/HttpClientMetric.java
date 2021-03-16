package com.sbss.bithon.agent.core.metrics.http;

import com.sbss.bithon.agent.core.metrics.Counter;

/**
 * @author frankchen
 */
public class HttpClientMetric {
    /**
     * HTTP URL
     */
    private final String uri;

    /**
     * HTTP METHOD
     */
    private final String method;

    /**
     * total cost time in NANO second
     */
    private final Counter costTime = new Counter();

    /**
     * count of all status code between 400(inclusive) and 500(exclusive)
     */
    private final Counter count4xx = new Counter();

    /**
     * count of all status code larger than 500(inclusive)
     */
    private final Counter count5xx = new Counter();
    private final Counter countException = new Counter();
    private final Counter requestCount = new Counter();
    private final Counter requestBytes = new Counter();
    private final Counter responseBytes = new Counter();

    public HttpClientMetric(String uri, String method) {
        this.uri = uri;
        this.method = method;
    }

    public void add(long costTime, int count4xx, int count5xx) {
        this.costTime.update(costTime);
        this.count4xx.update(count4xx);
        this.count5xx.update(count5xx);
        this.requestCount.incr();
    }

    public void addException(long costTime, int exceptionCount) {
        this.costTime.update(costTime);
        this.countException.update(exceptionCount);
        this.requestCount.incr();
    }

    public void addByteSize(long requestByteSize, long responseByteSize) {
        this.requestBytes.update(requestByteSize);
        this.responseBytes.update(responseByteSize);
    }

    public String getUri() {
        return uri;
    }

    public String getMethod() {
        return method;
    }

    public long getCostTime() {
        return costTime.get();
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
