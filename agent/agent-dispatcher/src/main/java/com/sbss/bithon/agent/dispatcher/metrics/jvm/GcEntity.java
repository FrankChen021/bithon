package com.sbss.bithon.agent.dispatcher.metrics.jvm;

import java.io.Serializable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:58 下午
 */
public class GcEntity implements Serializable {
    // 垃圾回收名称
    public String collectorName;

    // 垃圾回收次数
    public long gcCount;

    // 垃圾回收耗时
    public long gcTime;

    // GC分类0-新生代、1-老年代
    public long generation;
}
