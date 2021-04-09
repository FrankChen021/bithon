package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:32 下午
 */
public class LongMaxAggregator extends AbstractLongAggregator {

    public LongMaxAggregator() {
        this.value = Long.MIN_VALUE;
    }

    @Override
    protected void aggregate(long timestamp, long value) {
        this.value = Math.max(this.value, value);
    }

    @Override
    public int intValue() {
        return (int) (value == Long.MIN_VALUE ? 0 : value);
    }

    @Override
    public long longValue() {
        return (value == Long.MIN_VALUE ? 0 : value);
    }

    @Override
    public float floatValue() {
        return (value == Long.MIN_VALUE ? 0 : value);
    }

    @Override
    public double doubleValue() {
        return (value == Long.MIN_VALUE ? 0 : value);
    }
}
