package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:21 下午
 */
public interface IAggregator {
    void aggregate(long timestamp, Object value);

    Object getValue();
}
