package com.sbss.bithon.server.metric.aggregator.spec;

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
