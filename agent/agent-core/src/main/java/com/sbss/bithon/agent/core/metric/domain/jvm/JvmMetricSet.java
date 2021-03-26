package com.sbss.bithon.agent.core.metric.domain.jvm;


import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/10/27 2:17 下午
 */
public class JvmMetricSet {
    /**
     * uptime of the Java virtual machine in milliseconds.
     */
    public long upTime;

    /**
     * Returns the start time of the Java virtual machine in milliseconds.
     * This method returns the approximate time when the Java virtual machine started.
     */
    public long startTime;

    public CpuCompositeMetric cpuMetricsSet;
    public MemoryCompositeMetric memoryMetricsSet;
    public HeapCompositeMetric heapMetricsSet;
    public NonHeapCompositeMetric nonHeapMetricsSet;
    public List<GcCompositeMetric> gcCompositeMetrics;
    public ThreadCompositeMetric threadMetricsSet;
    public ClassCompositeMetric classMetricsSet;
    public MetaspaceCompositeMetric metaspaceMetricsSet;

    public JvmMetricSet(long upTime, long startTime) {
        this.upTime = upTime;
        this.startTime = startTime;
    }
}