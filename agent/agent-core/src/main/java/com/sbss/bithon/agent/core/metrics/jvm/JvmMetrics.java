package com.sbss.bithon.agent.core.metrics.jvm;


import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/10/27 2:17 下午
 */
public class JvmMetrics {
    // 应用实例运行时间，即不包含启动时间（单位：毫秒）
    public long upTime;

    // 系统正常运行时间，即包含启动时间（单位：毫秒）
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