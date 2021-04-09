package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:33 下午
 */
public class LongMinAggregator extends AbstractLongAggregator {

    public LongMinAggregator() {
        this.value = Long.MAX_VALUE;
    }

    @Override
    protected void aggregate(long timestamp, long value) {
        this.value = Math.min(this.value, value);
    }

    @Override
    public int intValue() {
        return (int) (value == Long.MAX_VALUE ? 0 : value);
    }

    @Override
    public long longValue() {
        return (value == Long.MAX_VALUE ? 0 : value);
    }

    @Override
    public float floatValue() {
        return (value == Long.MAX_VALUE ? 0 : value);
    }

    @Override
    public double doubleValue() {
        return (value == Long.MAX_VALUE ? 0 : value);
    }
}
