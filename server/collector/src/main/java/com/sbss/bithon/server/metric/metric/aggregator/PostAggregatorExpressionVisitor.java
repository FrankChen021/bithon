package com.sbss.bithon.server.metric.metric.aggregator;


import com.sbss.bithon.server.metric.metric.IMetricSpec;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/23
 */
public interface PostAggregatorExpressionVisitor {
    void visitMetric(IMetricSpec metricSpec);

    void visitConst(String constant);

    void visit(String operator);

    void startBrace();

    void endBrace();
}
