package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:28 下午
 */
public class DoubleSumAggregator implements IAggregator {
    private double value;

    @Override
    public void aggregate(long timestamp, Object value) {
        this.value += NumberUtils.getDouble(value);
    }

    @Override
    public Object getValue() {
        return value;
    }
}
