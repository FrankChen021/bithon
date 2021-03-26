package com.sbss.bithon.agent.core.metric.domain.jvm;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:55 下午
 */
public class ClassCompositeMetric {

    public long currentLoadedClasses;

    public long totalLoadedClasses;

    public long totalUnloadedClasses;

    public ClassCompositeMetric(long currentLoadedClasses, long totalLoadedClasses, long totalUnloadedClasses) {
        this.currentLoadedClasses = currentLoadedClasses;
        this.totalLoadedClasses = totalLoadedClasses;
        this.totalUnloadedClasses = totalUnloadedClasses;
    }
}
