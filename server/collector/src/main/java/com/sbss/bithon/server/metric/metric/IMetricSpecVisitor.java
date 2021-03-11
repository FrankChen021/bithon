package com.sbss.bithon.server.metric.metric;

import com.sbss.bithon.server.metric.metric.aggregator.PostAggregatorMetricSpec;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/23
 */
public interface IMetricSpecVisitor<T> {
    <T> T visit(LongSumMetricSpec metricSpec);

    <T> T visit(CountMetricSpec metricSpec);

    <T> T visit(DoubleSumMetricSpec metricSpec);

    <T> T visit(PostAggregatorMetricSpec metricSpec);

    <T> T visit(LongLastMetricSpec metricSpec);

    <T> T visit(DoubleLastMetricSpec metricSpec);
}
