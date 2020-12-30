package com.sbss.bithon.agent.dispatcher.metrics.jvm;

import java.io.Serializable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:56 下午
 */
public class ThreadEntity implements Serializable {

    // 从JVM启动或峰值重置以来峰值活动线程计数
    public long peakCount;

    // 活动守护线程的当前数目
    public long activeDaemonCount;

    // 从JVM启动以来创建和启动的线程总数目
    public long totalCreatedCount;

    // 活动线程的当前数目，包括守护线程和非守护线程
    public long acitveThreadsCount;

}
