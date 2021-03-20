package com.sbss.bithon.agent.core.metric.jvm;


import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/10/27 2:17 下午
 */
public class JvmMetrics {
    /**
     * uptime of the Java virtual machine in milliseconds.
     */
    public long upTime;

    /**
     * Returns the start time of the Java virtual machine in milliseconds.
     * This method returns the approximate time when the Java virtual machine started.
     */
    public long startTime;

    public CpuMetric cpuMetrics;
    public MemoryMetric memoryMetrics;
    public HeapMetric heapMetrics;
    public NonHeapMetric nonHeapMetrics;
    public List<GcMetric> gcMetrics;
    public ThreadMetric threadMetrics;
    public ClassMetric classMetrics;
    public MetaspaceMetric metaspaceMetrics;

    public JvmMetrics(long upTime, long startTime) {
        this.upTime = upTime;
        this.startTime = startTime;
    }
}