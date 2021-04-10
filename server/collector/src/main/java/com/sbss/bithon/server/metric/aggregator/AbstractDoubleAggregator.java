package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/9 19:24
 */
public abstract class AbstractDoubleAggregator extends NumberAggregator {

    protected double value;

    @Override
    public final void aggregate(long timestamp, Object value) {
        aggregate(timestamp, NumberUtils.getDouble(value));
    }

    protected abstract void aggregate(long timestamp, double value);

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return (long) value;
    }

    @Override
    public float floatValue() {
        return (float) value;
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
