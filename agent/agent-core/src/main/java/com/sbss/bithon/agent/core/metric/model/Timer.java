package com.sbss.bithon.agent.core.metric.model;

/**
 * It's a compound metric which holds total time, min time and max time
 *
 * @author frank.chen021@outlook.com
 * @date 2021-03-16
 */
public class Timer implements ISimpleMetric {

    private final Sum sum = new Sum();
    private final Max max = new Max();
    private final Min min = new Min();

    @Override
    public long update(long value) {
        max.update(value);
        min.update(value);
        return sum.update(value);
    }

    public Sum getSum() {
        return sum;
    }

    public Max getMax() {
        return max;
    }

    public Min getMin() {
        return min;
    }
}
