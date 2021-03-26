package com.sbss.bithon.agent.core.metric.model;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18-22:56
 */
public class Delta implements ISimpleMetric {
    private long oldValue;

    public Delta() {

    }

    public Delta(long initValue) {
        oldValue = initValue;
    }

    @Override
    public long update(long newValue) {
        long delta = newValue - oldValue;
        oldValue = newValue;
        return delta;
    }
}
