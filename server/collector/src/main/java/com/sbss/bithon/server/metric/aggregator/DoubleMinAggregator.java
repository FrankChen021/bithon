package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:33 下午
 */
public class DoubleMinAggregator extends AbstractDoubleAggregator {

    public DoubleMinAggregator() {
        this.value = Double.POSITIVE_INFINITY;
    }

    @Override
    public void aggregate(long timestamp, double value) {
        this.value = Math.min(this.value, value);
    }

    @Override
    public int intValue() {
        return (int) (value == Double.POSITIVE_INFINITY ? 0 : value);
    }

    @Override
    public long longValue() {
        return (long) (value == Double.POSITIVE_INFINITY ? 0 : value);
    }

    @Override
    public float floatValue() {
        return (float) (value == Double.POSITIVE_INFINITY ? 0 : value);
    }

    @Override
    public double doubleValue() {
        return (value == Double.POSITIVE_INFINITY ? 0 : value);
    }
}

