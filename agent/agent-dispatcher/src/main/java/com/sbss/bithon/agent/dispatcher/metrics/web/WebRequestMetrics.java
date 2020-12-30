package com.sbss.bithon.agent.dispatcher.metrics.web;

import com.keruyun.commons.agent.collector.entity.RequestPerformanceEntity;

/**
 * Description : 请求统计器的存储
 * <br>Date: 17/10/30
 *
 * @author 马至远
 */
public class WebRequestMetrics {
    /**
     * 接口唯一标识, uri
     */
    private String uri;
    /**
     * 接口耗时总值
     */
    private long cost = 0;

    /**
     * 请求量总值
     */
    private int requests = 0;

    /**
     * 请求失败总值
     */
    private int failureCount = 0;
    /**
     * 40x状态值
     */
    private int failure40xCount = 0;
    /**
     * 50x状态值
     */
    private int failure50xCount = 0;
    /**
     * 请求ByteSize
     */
    private long requestByteSize = 0;
    /**
     * 响应ByteSize
     */
    private long responseByteSize = 0;


    public WebRequestMetrics(String uri) {
        this.uri = uri;
    }

    public void add(long cost, int failureCount) {
        this.cost += cost;
        this.requests++;
        this.failureCount += failureCount;
    }


    public void add(long cost, int failureCount, int failure40xCount, int failure50xCount) {
        this.add(cost, failureCount);
        this.failure40xCount += failure40xCount;
        this.failure50xCount += failure50xCount;
    }

    public void addByteSize(long requestByteSize, long responseByteSize) {
        if (requestByteSize > 0) {
            this.requestByteSize += requestByteSize;
        }
        if (responseByteSize > 0) {
            this.responseByteSize += responseByteSize;
        }
    }


    /**
     * 获取标准化thrift数据 - RequestPerformanceEntity
     *
     * @return requestPerformanceEntity
     */
    public RequestPerformanceEntity getFormattingRequestPerformance() {
        return new RequestPerformanceEntity(uri, cost, requests, failureCount, failure40xCount, failure50xCount, requestByteSize, responseByteSize);
    }
}
