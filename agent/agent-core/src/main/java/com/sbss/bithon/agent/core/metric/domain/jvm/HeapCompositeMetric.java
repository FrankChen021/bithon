package com.sbss.bithon.agent.core.metric.domain.jvm;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:52 下午
 */
public class HeapCompositeMetric {
    /**
     * approximate to -Xmx
     */
    public long heapBytes;
    /**
     * approximate to -Xms
     */
    public long heapInitBytes;
    public long heapUsedBytes;
    public long heapAvailableBytes;

    public HeapCompositeMetric(long heapBytes, long heapInitBytes, long heapUsedBytes, long heapAvailableBytes) {
        this.heapBytes = heapBytes;
        this.heapInitBytes = heapInitBytes;
        this.heapUsedBytes = heapUsedBytes;
        this.heapAvailableBytes = heapAvailableBytes;
    }
}
