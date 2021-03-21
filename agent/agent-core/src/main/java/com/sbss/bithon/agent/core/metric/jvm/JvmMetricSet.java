package com.sbss.bithon.agent.core.metric.jvm;


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

    public CpuMetricSet cpuMetricsSet;
    public MemoryMetricSet memoryMetricsSet;
    public HeapMetricSet heapMetricsSet;
    public NonHeapMetricSet nonHeapMetricsSet;
    public List<GcMetricSet> gcMetricSets;
    public ThreadMetricSet threadMetricsSet;
    public ClassMetricSet classMetricsSet;
    public MetaspaceMetricSet metaspaceMetricsSet;

    public JvmMetricSet(long upTime, long startTime) {
        this.upTime = upTime;
        this.startTime = startTime;
    }
}