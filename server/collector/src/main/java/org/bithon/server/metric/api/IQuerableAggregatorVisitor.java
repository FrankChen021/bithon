package org.bithon.server.metric.api;

/**
 * @author Frank Chen
 * @date 1/11/21 3:12 pm
 */
public interface IQuerableAggregatorVisitor<T> {
    T visit(CardinalityAggregator aggregator);
}
