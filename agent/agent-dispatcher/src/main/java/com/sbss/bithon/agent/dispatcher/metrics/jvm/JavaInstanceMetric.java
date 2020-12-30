package com.sbss.bithon.agent.dispatcher.metrics.jvm;


import com.sbss.bithon.agent.dispatcher.metrics.MetricBase;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/10/27 2:17 下午
 */
public class JavaInstanceMetric extends MetricBase {
    // 应用实例运行时间，即不包含启动时间（单位：毫秒）
    private long upTime;

    // 系统正常运行时间，即包含启动时间（单位：毫秒）
    private long startTime;

    public CpuEntity cpuEntity;
    public MemoryEntity memoryEntity;
    public HeapEntity heapEntity;
    public NonHeapEntity nonHeapEntity;
    public List<GcEntity> gcEntity;
    public ThreadEntity threadEntity;
    public ClassEntity classesEntity;
    public MetaspaceEntity metaspaceEntity;
}