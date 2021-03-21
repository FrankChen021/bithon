package com.sbss.bithon.agent.core.metric.jvm;

/**
 * @author: frank.chen021@outlook.com
 * @date: 2020/12/29 9:54 下午
 */
public class NonHeapMetricSet {
    // approximate to -XX:MaxPermSize
    public long nonHeapBytes;

    // approximate to -XX:PermSize
    public long nonHeapInitBytes;

    public long nonHeapUsedBytes;

    // available memory including used
    public long nonHeapAvailableBytes;

    public NonHeapMetricSet(long nonHeapBytes,
                            long nonHeapInitBytes,
                            long nonHeapUsedBytes,
                            long nonHeapAvailableBytes) {
        this.nonHeapBytes = nonHeapBytes;
        this.nonHeapInitBytes = nonHeapInitBytes;
        this.nonHeapUsedBytes = nonHeapUsedBytes;
        this.nonHeapAvailableBytes = nonHeapAvailableBytes;
    }
}
