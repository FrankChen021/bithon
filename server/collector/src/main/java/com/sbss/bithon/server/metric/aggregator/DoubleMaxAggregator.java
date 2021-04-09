package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:32 下午
 */
public class DoubleMaxAggregator extends AbstractDoubleAggregator {

    public DoubleMaxAggregator() {
        this.value = Double.MIN_VALUE;
    }

    @Override
    protected void aggregate(long timestamp, double value) {
        this.value = Math.max(this.value, value);
    }

    @Override
    public int intValue() {
        return (int) (value == Double.MIN_VALUE ? 0 : value);
    }

    @Override
    public long longValue() {
        return (long) (value == Double.MIN_VALUE ? 0 : value);
    }

    @Override
    public float floatValue() {
        return (float) (value == Double.MIN_VALUE ? 0 : value);
    }

    @Override
    public double doubleValue() {
        return (value == Double.MIN_VALUE ? 0 : value);
    }
}
