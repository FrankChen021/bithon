/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.metric.expression.pipeline;


import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.pipeline.IQueryStep;
import org.bithon.server.metric.expression.ast.IMetricExpressionVisitor;
import org.bithon.server.metric.expression.ast.MetricExpression;
import org.bithon.server.metric.expression.ast.MetricExpressionASTBuilder;
import org.bithon.server.metric.expression.ast.MetricExpressionOptimizer;
import org.bithon.server.metric.expression.pipeline.step.BinaryExpressionQueryStep;
import org.bithon.server.metric.expression.pipeline.step.FilterStep;
import org.bithon.server.metric.expression.pipeline.step.LiteralQueryStep;
import org.bithon.server.metric.expression.pipeline.step.MetricExpressionQueryStep;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryField;
import org.bithon.server.web.service.datasource.api.QueryRequest;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:53 pm
 */
public class QueryPipelineBuilder {

    private IDataSourceApi dataSourceApi;
    private IntervalRequest intervalRequest;
    private String condition;

    public static QueryPipelineBuilder builder() {
        return new QueryPipelineBuilder();
    }

    public QueryPipelineBuilder dataSourceApi(IDataSourceApi dataSourceApi) {
        this.dataSourceApi = dataSourceApi;
        return this;
    }

    public QueryPipelineBuilder intervalRequest(IntervalRequest intervalRequest) {
        this.intervalRequest = intervalRequest;
        return this;
    }

    public QueryPipelineBuilder condition(String condition) {
        this.condition = condition;
        return this;
    }

    public IQueryStep build(String expression) {
        IExpression expr = MetricExpressionASTBuilder.parse(expression);

        // Apply optimization like constant folding on parsed expression
        // The optimization is applied here so that above parse can be tested separately
        expr = MetricExpressionOptimizer.optimize(expr);

        return this.build(expr);
    }

    public IQueryStep build(IExpression expression) {
        return expression.accept(new Builder());
    }

    private class Builder implements IMetricExpressionVisitor<IQueryStep> {
        @Override
        public IQueryStep visit(MetricExpression expression) {
            String filterExpression = Stream.of(expression.getWhereText(), condition)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.joining(" AND "));

            if (expression.getOffset() != null) {
                // Create expression as: ( current - base ) / base
                QueryField metricField = expression.getMetric();
                String expr = StringUtils.format("%s(%s) * 1.0", metricField.getAggregator(), metricField.getField());
                MetricExpressionQueryStep curr = new MetricExpressionQueryStep(QueryRequest.builder()
                                                                                           .dataSource(expression.getFrom())
                                                                                           .filterExpression(filterExpression)
                                                                                           .groupBy(expression.getGroupBy())
                                                                                           .fields(List.of(new QueryField(metricField.getName(), metricField.getField(), null, expr)))
                                                                                           .interval(intervalRequest)
                                                                                           .build(),
                                                                               dataSourceApi);

                MetricExpressionQueryStep base = new MetricExpressionQueryStep(QueryRequest.builder()
                                                                                           .dataSource(expression.getFrom())
                                                                                           .filterExpression(filterExpression)
                                                                                           .groupBy(expression.getGroupBy())
                                                                                           .fields(List.of(new QueryField(
                                                                                               // Use offset AS the output name
                                                                                               expression.getOffset().toString(),
                                                                                               metricField.getField(),
                                                                                               null,
                                                                                               expr)))
                                                                                           .interval(intervalRequest)
                                                                                           .offset(expression.getOffset())
                                                                                           .build(),
                                                                               dataSourceApi);

                //
                return new BinaryExpressionQueryStep.Div(
                    new BinaryExpressionQueryStep.Sub(
                        curr,
                        base,
                        "diff",

                        // Returns 'curr' as well as the computed result set
                        metricField.getName()
                    ),
                    base,
                    "delta",

                    // Keep the current and base columns in the result set
                    metricField.getName(), expression.getOffset().toString()
                );
            } else {
                return new MetricExpressionQueryStep(QueryRequest.builder()
                                                                 .dataSource(expression.getFrom())
                                                                 .filterExpression(filterExpression)
                                                                 .groupBy(expression.getGroupBy())
                                                                 .fields(List.of(expression.getMetric()))
                                                                 .interval(intervalRequest)
                                                                 .build(),
                                                     dataSourceApi);
            }
        }

        @Override
        public IQueryStep visit(LiteralExpression<?> expression) {
            return new LiteralQueryStep(expression, Interval.of(intervalRequest.getStartISO8601(),
                                                                intervalRequest.getEndISO8601(),
                                                                intervalRequest.calculateStep(),
                                                                null));
        }

        @Override
        public IQueryStep visit(ArithmeticExpression expression) {
            return switch (expression.getType()) {
                case "+" -> new BinaryExpressionQueryStep.Add(expression.getLhs().accept(this), expression.getRhs().accept(this));
                case "-" -> new BinaryExpressionQueryStep.Sub(expression.getLhs().accept(this), expression.getRhs().accept(this));
                case "*" -> new BinaryExpressionQueryStep.Mul(expression.getLhs().accept(this), expression.getRhs().accept(this));
                case "/" -> new BinaryExpressionQueryStep.Div(expression.getLhs().accept(this), expression.getRhs().accept(this));
                default -> throw new UnsupportedOperationException("Unsupported arithmetic expression: " + expression.getType());
            };
        }

        @Override
        public IQueryStep visit(ConditionalExpression expression) {
            IQueryStep source = expression.getLhs().accept(this);
            if (expression instanceof ComparisonExpression.LT) {
                return new FilterStep.LT(
                    source,
                    (LiteralExpression<?>) expression.getRhs()
                );
            }
            if (expression instanceof ComparisonExpression.LTE) {
                return new FilterStep.LTE(
                    source,
                    (LiteralExpression<?>) expression.getRhs()
                );
            }
            if (expression instanceof ComparisonExpression.GT) {
                return new FilterStep.GT(
                    source,
                    (LiteralExpression<?>) expression.getRhs()
                );
            }
            if (expression instanceof ComparisonExpression.GTE) {
                return new FilterStep.GTE(
                    source,
                    (LiteralExpression<?>) expression.getRhs()
                );
            }
            if (expression instanceof ComparisonExpression.NE) {
                return new FilterStep.NE(
                    source,
                    (LiteralExpression<?>) expression.getRhs()
                );
            }
            throw new UnsupportedOperationException("Unsupported conditional expression: " + expression.getType());
        }
    }
}
