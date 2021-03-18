package com.sbss.bithon.agent.core.metrics.http;

import com.sbss.bithon.agent.core.metrics.Compund;
import com.sbss.bithon.agent.core.metrics.Sum;

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
    private final Compund responseTime = new Compund();

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

    public HttpClientMetric(String uri, String method) {
        this.uri = uri;
        this.method = method;
    }

    public void add(long responseTime, int count4xx, int count5xx) {
        this.responseTime.update(responseTime);
        this.count4xx.update(count4xx);
        this.count5xx.update(count5xx);
        this.requestCount.incr();
    }

    public void addException(long costTime, int exceptionCount) {
        this.responseTime.update(costTime);
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

    public Compund getResponseTime() {
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
