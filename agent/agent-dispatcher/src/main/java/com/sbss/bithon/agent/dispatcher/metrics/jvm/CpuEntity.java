package com.sbss.bithon.agent.dispatcher.metrics.jvm;

import java.io.Serializable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:49 下午
 */
public class CpuEntity implements Serializable {
    // CPU内核数
    private long processorNumber;

    // CPU处理时间（单位：纳秒）
    private long processCpuTime;

    // 处理器的负载均值
    private double avgSystemLoad;

    // cpu使用情况（单位：百分比）
    private double processCpuLoad;
}