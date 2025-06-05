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
import org.bithon.server.datasource.query.plan.physical.ArithmeticStep;
import org.bithon.server.datasource.query.plan.physical.FilterStep;
import org.bithon.server.datasource.query.plan.physical.IPhysicalPlan;
import org.bithon.server.datasource.query.plan.physical.LiteralQueryStep;
import org.bithon.server.datasource.query.plan.physical.MetricQueryStep;
import org.bithon.server.metric.expression.ast.IMetricExpressionVisitor;
import org.bithon.server.metric.expression.ast.MetricAggregateExpression;
import org.bithon.server.metric.expression.ast.MetricExpectedExpression;
import org.bithon.server.metric.expression.ast.MetricExpressionASTBuilder;
import org.bithon.server.metric.expression.ast.MetricExpressionOptimizer;
import org.bithon.server.metric.expression.ast.PredicateEnum;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryField;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:53 pm
 */
public class PhysicalPlanner {

    private IDataSourceApi dataSourceApi;
    private IntervalRequest intervalRequest;
    private String condition;
    private QueryPipelineBuilderSettings settings = QueryPipelineBuilderSettings.DEFAULT;

    public static PhysicalPlanner builder() {
        return new PhysicalPlanner();
    }

    public PhysicalPlanner dataSourceApi(IDataSourceApi dataSourceApi) {
        this.dataSourceApi = dataSourceApi;
        return this;
    }

    public PhysicalPlanner intervalRequest(IntervalRequest intervalRequest) {
        this.intervalRequest = intervalRequest;
        return this;
    }

    public PhysicalPlanner condition(String condition) {
        this.condition = condition;
        return this;
    }

    public PhysicalPlanner settings(QueryPipelineBuilderSettings settings) {
        this.settings = settings;
        return this;
    }

    public IPhysicalPlan build(String expression) {
        IExpression expr = MetricExpressionASTBuilder.parse(expression);

        // Apply optimization like constant folding on parsed expression
        // The optimization is applied here so that the above parse can be tested separately
        expr = MetricExpressionOptimizer.optimize(expr);

        return this.build(expr);
    }

    public IPhysicalPlan build(IExpression expression) {
        return expression.accept(new Builder());
    }

    private class Builder implements IMetricExpressionVisitor<IPhysicalPlan> {
        @Override
        public IPhysicalPlan visit(MetricAggregateExpression expression) {
            String filterExpression = Stream.of(expression.getWhereText(), condition)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.joining(" AND "));

            if (expression.getOffset() != null) {
                // Create expression as: (current - base) / base
                QueryField metricField = expression.getMetric();
                String expr = StringUtils.format("%s(%s) * 1.0", metricField.getAggregator(), metricField.getField());
                MetricQueryStep curr = MetricQueryStep.builder()
                                                      .dataSource(expression.getFrom())
                                                      .filterExpression(filterExpression)
                                                      .groupBy(expression.getGroupBy())
                                                      /*
                                                      .fields(List.of(new QueryField(metricField.getName(), metricField.getField(), null, expr)))
                                                      .interval(intervalRequest)
                                                      .dataSourceApi(dataSourceApi)*/
                                                      .build();

                MetricQueryStep base = MetricQueryStep.builder()
                                                      .dataSource(expression.getFrom())
                                                      .filterExpression(filterExpression)
                                                      .groupBy(expression.getGroupBy())
                                                      /*
                                                      .fields(List.of(new QueryField(
                                                          // Use offset AS the output name
                                                          expression.getOffset().toString(),
                                                          metricField.getField(),
                                                          null,
                                                          expr)))
                                                      .interval(intervalRequest)
                                                      .offset(expression.getOffset())
                                                      .dataSourceApi(dataSourceApi)*/
                                                      .build();

                //
                return new ArithmeticStep.Div(
                    new ArithmeticStep.Sub(
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
                return MetricQueryStep.builder()
                                      .dataSource(expression.getFrom())
                                      .filterExpression(filterExpression)
                                      .groupBy(expression.getGroupBy())
                                      /*
                                      .fields(List.of(expression.getMetric()))
                                      .interval(intervalRequest)
                                      .dataSourceApi(dataSourceApi)*/
                                      .build();
            }
        }

        /*
        @Override
        public IQueryStep visit(FunctionCallExpression expression) {
            if (expression.getArguments() instanceof MetricSelectExpression selectExpression) {
                AggregatorEnum aggregator = AggregatorEnum.fromString(expression.getOperator());
                if (aggregator != null) {
                    String filterExpression = Stream.of(selectExpression.getWhereText(), condition)
                                                    .filter(Objects::nonNull)
                                                    .collect(Collectors.joining(" AND "));
                    return new MetricQueryStep(QueryRequest.builder()
                                                           .dataSource(selectExpression.getFrom())
                                                           .filterExpression(filterExpression)
                                                           .groupBy(expression.getGroupBy())
                                                           .fields(List.of(new QueryField(selectExpression.getMetric(),
                                                                                          selectExpression.getMetric(),
                                                                                          aggregator.name())))
                                                           .interval(intervalRequest)
                                                           .build(),
                                               dataSourceApi);
                } else {
                    // other functions
                    throw new UnsupportedOperationException("Unsupported metric call expression: " + expression.getOperator());
                }
            } else {
                throw new UnsupportedOperationException("Unsupported metric call expression: " + expression.getArguments().getClass().getSimpleName());
            }
        }*/

        @Override
        public IPhysicalPlan visit(LiteralExpression<?> expression) {
            return new LiteralQueryStep(expression, Interval.of(intervalRequest.getStartISO8601(),
                                                                intervalRequest.getEndISO8601(),
                                                                intervalRequest.calculateStep(),
                                                                null));
        }

        @Override
        public IPhysicalPlan visit(ArithmeticExpression expression) {
            return switch (expression.getType()) {
                case "+" -> new ArithmeticStep.Add(expression.getLhs().accept(this), expression.getRhs().accept(this));
                case "-" -> new ArithmeticStep.Sub(expression.getLhs().accept(this), expression.getRhs().accept(this));
                case "*" -> new ArithmeticStep.Mul(expression.getLhs().accept(this), expression.getRhs().accept(this));
                case "/" -> new ArithmeticStep.Div(expression.getLhs().accept(this), expression.getRhs().accept(this));
                default ->
                    throw new UnsupportedOperationException("Unsupported arithmetic expression: " + expression.getType());
            };
        }

        /**
         * Push down the filter condition to the source step.
         */
        @Override
        public IPhysicalPlan visit(ConditionalExpression expression) {
            if (settings.isPushdownPostFilter()) {
                IExpression lhs = expression.getLhs();
                IExpression rhs = expression.getRhs();
                if (lhs instanceof MetricAggregateExpression metricExpression
                    && rhs instanceof MetricExpectedExpression expectedExpression) {
                    if (expression instanceof ComparisonExpression.LT) {
                        metricExpression.setPredicate(PredicateEnum.LT);
                    } else if (expression instanceof ComparisonExpression.GT) {
                        metricExpression.setPredicate(PredicateEnum.GT);
                    } else if (expression instanceof ComparisonExpression.LTE) {
                        metricExpression.setPredicate(PredicateEnum.LTE);
                    } else if (expression instanceof ComparisonExpression.GTE) {
                        metricExpression.setPredicate(PredicateEnum.GTE);
                    } else if (expression instanceof ComparisonExpression.EQ) {
                        metricExpression.setPredicate(PredicateEnum.EQ);
                    } else if (expression instanceof ComparisonExpression.NE) {
                        metricExpression.setPredicate(PredicateEnum.NE);
                    } else {
                        throw new UnsupportedOperationException("Unsupported comparison expression: " + expression.getClass().getSimpleName());
                    }

                    metricExpression.setExpected(expectedExpression.getExpected());
                    metricExpression.setOffset(expectedExpression.getOffset());
                    return this.visit(metricExpression);
                }
            }

            IPhysicalPlan source = expression.getLhs().accept(this);

            if (expression instanceof ComparisonExpression.LT) {
                return new FilterStep.LT(
                    source,
                    (Number) ((MetricExpectedExpression) expression.getRhs()).getExpected().getValue()
                );
            }
            if (expression instanceof ComparisonExpression.LTE) {
                return new FilterStep.LTE(
                    source,
                    (Number) ((MetricExpectedExpression) expression.getRhs()).getExpected().getValue()
                );
            }
            if (expression instanceof ComparisonExpression.GT) {
                return new FilterStep.GT(
                    source,
                    (Number) ((MetricExpectedExpression) expression.getRhs()).getExpected().getValue()
                );
            }
            if (expression instanceof ComparisonExpression.GTE) {
                return new FilterStep.GTE(
                    source,
                    (Number) ((MetricExpectedExpression) expression.getRhs()).getExpected().getValue()
                );
            }
            if (expression instanceof ComparisonExpression.NE) {
                return new FilterStep.NE(
                    source,
                    (Number) ((MetricExpectedExpression) expression.getRhs()).getExpected().getValue()
                );
            }
            throw new UnsupportedOperationException("Unsupported conditional expression: " + expression.getType());
        }
    }
}
