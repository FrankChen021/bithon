package com.sbss.bithon.agent.dispatcher.metrics.http;

/**
 * Description : client请求的计数器存储
 * <br>Date: 17/10/31
 *
 * @author 马至远
 */
public class HttpMetrics {
    /**
     * 请求uri
     */
    private String uri;

    /**
     * 请求method
     */
    private String method;

    /**
     * 消耗的总时间
     */
    private long costTime = 0;

    /**
     * 4xx失败数
     */
    private int failureCount = 0;

    /**
     * 5xx失败数
     */
    private int errorCount = 0;

    /**
     * 请求总数
     */
    private int requestCount = 0;
    /**
     * 请求ByteSize
     */
    private long requestByteSize = 0;
    /**
     * 响应ByteSize
     */
    private long responseByteSize = 0;

    public HttpMetrics(String uri, String method) {
        this.uri = uri;
        this.method = method;
    }

    public void add(long costTime, int failureCount, int errorCount) {
        this.costTime += costTime;
        this.failureCount += failureCount;
        this.errorCount += errorCount;
        this.requestCount++;
    }

    public void addByteSize(long requestByteSize, long responseByteSize) {
        this.requestByteSize += requestByteSize;
        this.responseByteSize += responseByteSize;
    }

    public String getUri() {
        return uri;
    }

    public String getMethod() {
        return method;
    }

    public long getCostTime() {
        return costTime;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public long getRequestByteSize() {
        return requestByteSize;
    }

    public void setRequestByteSize(long requestByteSize) {
        this.requestByteSize = requestByteSize;
    }

    public long getResponseByteSize() {
        return responseByteSize;
    }

    public void setResponseByteSize(long responseByteSize) {
        this.responseByteSize = responseByteSize;
    }
}
