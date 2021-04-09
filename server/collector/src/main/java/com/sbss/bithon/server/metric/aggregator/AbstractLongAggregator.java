package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/9 19:24
 */
public abstract class AbstractLongAggregator extends NumberAggregator {

    protected long value;

    @Override
    final public void aggregate(long timestamp, Object value) {
        aggregate(timestamp, NumberUtils.getLong(value));
    }

    abstract protected void aggregate(long timestamp, long value);

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
