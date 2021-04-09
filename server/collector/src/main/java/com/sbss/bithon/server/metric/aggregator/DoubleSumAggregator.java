package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:28 下午
 */
public class DoubleSumAggregator extends AbstractDoubleAggregator {
    @Override
    protected void aggregate(long timestamp, double value) {
        this.value += value;
    }
}
