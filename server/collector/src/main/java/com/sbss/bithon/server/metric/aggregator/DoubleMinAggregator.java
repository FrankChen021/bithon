package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:33 下午
 */
public class DoubleMinAggregator implements IAggregator {
    private double value = Double.MAX_VALUE;

    @Override
    public void aggregate(long timestamp, Object value) {
        this.value = Math.min(this.value, NumberUtils.getDouble(value));
    }

    @Override
    public Object getValue() {
        return value;
    }
}
