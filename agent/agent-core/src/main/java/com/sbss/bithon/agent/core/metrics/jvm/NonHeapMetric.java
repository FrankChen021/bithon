package com.sbss.bithon.agent.core.metrics.jvm;

import java.io.Serializable;

/**
 * @author: frank.chen021@outlook.com
 * @date: 2020/12/29 9:54 下午
 */
public class NonHeapMetric implements Serializable {
    // approximate to -XX:MaxPermSize
    public long nonHeapBytes;

    // approximate to -XX:PermSize
    public long nonHeapInitBytes;

    public long nonHeapUsedBytes;

    // available memory including used
    public long nonHeapCommitted;

    public NonHeapMetric(long nonHeapBytes,
                         long nonHeapInitBytes,
                         long nonHeapUsedBytes,
                         long nonHeapCommitted) {
        this.nonHeapBytes = nonHeapBytes;
        this.nonHeapInitBytes = nonHeapInitBytes;
        this.nonHeapUsedBytes = nonHeapUsedBytes;
        this.nonHeapCommitted = nonHeapCommitted;
    }
}
