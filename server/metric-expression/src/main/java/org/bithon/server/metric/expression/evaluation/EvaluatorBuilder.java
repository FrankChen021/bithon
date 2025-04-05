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

package org.bithon.server.metric.expression.evaluation;


import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.server.metric.expression.ast.IMetricExpressionVisitor;
import org.bithon.server.metric.expression.ast.MetricExpression;
import org.bithon.server.metric.expression.ast.MetricExpressionASTBuilder;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryRequest;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:53 pm
 */
public class EvaluatorBuilder {

    private IDataSourceApi dataSourceApi;
    private IntervalRequest intervalRequest;
    private String condition;

    public static EvaluatorBuilder builder() {
        return new EvaluatorBuilder();
    }

    public EvaluatorBuilder dataSourceApi(IDataSourceApi dataSourceApi) {
        this.dataSourceApi = dataSourceApi;
        return this;
    }

    public EvaluatorBuilder intervalRequest(IntervalRequest intervalRequest) {
        this.intervalRequest = intervalRequest;
        return this;
    }

    public EvaluatorBuilder condition(String condition) {
        this.condition = condition;
        return this;
    }

    public IEvaluator build(String expression) {
        return this.build(MetricExpressionASTBuilder.parse(expression));
    }

    public IEvaluator build(IExpression expression) {
        return expression.accept(new Builder());
    }

    private class Builder implements IMetricExpressionVisitor<IEvaluator> {
        @Override
        public IEvaluator visit(MetricExpression expression) {
            String filterExpression = Stream.of(expression.getWhereText(), condition)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.joining(" AND "));

            QueryRequest req = QueryRequest.builder()
                                           .dataSource(expression.getFrom())
                                           .filterExpression(filterExpression)
                                           .groupBy(expression.getGroupBy())
                                           .fields(List.of(expression.getMetric()))
                                           .interval(intervalRequest)
                                           .build();

            if (expression.getOffset() != null) {
                // Create expression as: ( current - base ) / base
                // TODO: produce the delta and base series in the result
                MetricExpressionEvaluator base = new MetricExpressionEvaluator(QueryRequest.builder()
                                                                                           .dataSource(expression.getFrom())
                                                                                           .filterExpression(filterExpression)
                                                                                           .groupBy(expression.getGroupBy())
                                                                                           .fields(List.of(expression.getMetric()))
                                                                                           .interval(intervalRequest)
                                                                                           .offset(expression.getOffset())
                                                                                           .build(),
                                                                               dataSourceApi);
                return new BinaryExpressionEvaluator.Div(
                    new BinaryExpressionEvaluator.Sub(
                        new MetricExpressionEvaluator(req, dataSourceApi),
                        base,
                        "delta",
                        true
                    ),
                    base,
                    null,
                    true
                );
            } else {
                return new MetricExpressionEvaluator(req, dataSourceApi);
            }
        }

        @Override
        public IEvaluator visit(LiteralExpression<?> expression) {
            return new LiteralEvaluator(expression);
        }

        @Override
        public IEvaluator visit(ArithmeticExpression expression) {
            return switch (expression.getType()) {
                case "+" -> new BinaryExpressionEvaluator.Add(expression.getLhs().accept(this), expression.getRhs().accept(this));
                case "-" -> new BinaryExpressionEvaluator.Sub(expression.getLhs().accept(this), expression.getRhs().accept(this));
                case "*" -> new BinaryExpressionEvaluator.Mul(expression.getLhs().accept(this), expression.getRhs().accept(this));
                case "/" -> new BinaryExpressionEvaluator.Div(expression.getLhs().accept(this), expression.getRhs().accept(this));
                default -> throw new UnsupportedOperationException("Unsupported arithmetic expression: " + expression.getType());
            };
        }
    }
}
