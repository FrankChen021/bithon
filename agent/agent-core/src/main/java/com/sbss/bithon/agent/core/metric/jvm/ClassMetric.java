package com.sbss.bithon.agent.core.metric.jvm;

import java.io.Serializable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:55 下午
 */
public class ClassMetric implements Serializable {

    public long currentLoadedClasses;

    public long totalLoadedClasses;

    public long totalUnloadedClasses;

    public ClassMetric(long currentLoadedClasses, long totalLoadedClasses, long totalUnloadedClasses) {
        this.currentLoadedClasses = currentLoadedClasses;
        this.totalLoadedClasses = totalLoadedClasses;
        this.totalUnloadedClasses = totalUnloadedClasses;
    }
}
