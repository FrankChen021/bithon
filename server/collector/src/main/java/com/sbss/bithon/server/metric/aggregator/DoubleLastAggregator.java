package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:26 下午
 */
public class DoubleLastAggregator implements IAggregator {
    private long timestamp = Long.MIN_VALUE;
    private double value;

    @Override
    public void aggregate(long timestamp, Object value) {
        if (this.timestamp < timestamp) {
            this.timestamp = timestamp;
            this.value = NumberUtils.getDouble(value);
        }
    }

    @Override
    public Object getValue() {
        return value;
    }
}
