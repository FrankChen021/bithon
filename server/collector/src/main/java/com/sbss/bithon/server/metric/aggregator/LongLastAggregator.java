package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:26 下午
 */
public class LongLastAggregator extends AbstractLongAggregator {
    private long timestamp = Long.MIN_VALUE;

    @Override
    protected void aggregate(long timestamp, long value) {
        if (this.timestamp < timestamp) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }

    @Override
    public Object getValue() {
        return value;
    }
}
