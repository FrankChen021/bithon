package com.sbss.bithon.server.metric.aggregator;


/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/23
 */
public interface PostAggregatorExpressionVisitor {
    void visitMetric(IMetricSpec metricSpec);

    void visitNumber(String number);

    void visitorOperator(String operator);

    void startBrace();

    void endBrace();

    void visitVariable(String variable);
}
