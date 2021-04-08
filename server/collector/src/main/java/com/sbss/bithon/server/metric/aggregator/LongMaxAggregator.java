package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:32 下午
 */
public class LongMaxAggregator implements IAggregator {
    private long value = Long.MIN_VALUE;

    @Override
    public void aggregate(long timestamp, Object value) {
        this.value = Math.max(this.value, NumberUtils.getLong(value));
    }

    @Override
    public Object getValue() {
        return value;
    }
}
