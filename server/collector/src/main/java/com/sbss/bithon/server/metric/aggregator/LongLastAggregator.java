package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:26 下午
 */
public class LongLastAggregator implements IAggregator {
    private long timestamp = Long.MIN_VALUE;
    private long value;

    @Override
    public void aggregate(long timestamp, Object value) {
        if (this.timestamp < timestamp) {
            this.timestamp = timestamp;
            this.value = NumberUtils.getLong(value);
        }
    }

    @Override
    public Object getValue() {
        return value;
    }
}
