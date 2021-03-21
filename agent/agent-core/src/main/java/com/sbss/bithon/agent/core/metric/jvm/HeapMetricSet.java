package com.sbss.bithon.agent.core.metric.jvm;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:52 下午
 */
public class HeapMetricSet {
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

    public HeapMetricSet(long heapBytes, long heapInitBytes, long heapUsedBytes, long heapAvailableBytes) {
        this.heapBytes = heapBytes;
        this.heapInitBytes = heapInitBytes;
        this.heapUsedBytes = heapUsedBytes;
        this.heapAvailableBytes = heapAvailableBytes;
    }
}
