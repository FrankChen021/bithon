package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:33 下午
 */
public class DoubleMinAggregator extends AbstractDoubleAggregator {

    @Override
    public void aggregate(long timestamp, double value) {
        this.value = Math.min(this.value, value);
    }
}

