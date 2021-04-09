package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:32 下午
 */
public class LongMaxAggregator extends AbstractLongAggregator {
    @Override
    protected void aggregate(long timestamp, long value) {
        this.value = Math.max(this.value, value);
    }
}
