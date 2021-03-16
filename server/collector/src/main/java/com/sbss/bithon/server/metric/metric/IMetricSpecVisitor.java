package com.sbss.bithon.server.metric.metric;

import com.sbss.bithon.server.metric.metric.aggregator.PostAggregatorMetricSpec;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/23
 */
public interface IMetricSpecVisitor<T> {
    T visit(LongSumMetricSpec metricSpec);

    T visit(CountMetricSpec metricSpec);

    T visit(DoubleSumMetricSpec metricSpec);

    T visit(PostAggregatorMetricSpec metricSpec);

    T visit(LongLastMetricSpec metricSpec);

    T visit(DoubleLastMetricSpec metricSpec);

    T visit(LongMinMetricSpec metricSpec);

    T visit(LongMaxMetricSpec metricSpec);
}
