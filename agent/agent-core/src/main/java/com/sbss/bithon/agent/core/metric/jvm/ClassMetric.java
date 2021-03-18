package com.sbss.bithon.agent.core.metric.jvm;

import java.io.Serializable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:55 下午
 */
public class ClassMetric implements Serializable {

    // 当前加载到JVM中的类的数量
    public long currentClassCount;

    // 自JVM开始执行到目前已经加载的类的总数
    public long loadedClassCount;

    // 自JVM开始执行到目前已经卸载的类的总数
    public long unloadedClassCount;

    public ClassMetric(long currentClassCount, long loadedClassCount, long unloadedClassCount) {
        this.currentClassCount = currentClassCount;
        this.loadedClassCount = loadedClassCount;
        this.unloadedClassCount = unloadedClassCount;
    }
}
