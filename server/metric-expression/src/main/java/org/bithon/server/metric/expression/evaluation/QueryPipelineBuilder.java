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
import org.bithon.server.metric.expression.MetricExpression;
import org.bithon.server.metric.expression.MetricExpressionASTBuilder;
import org.bithon.server.metric.expression.ast.IMetricExpressionVisitor;
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

    public IPipeline build(String expression) {
        return this.build(MetricExpressionASTBuilder.parse(expression));
    }

    public IPipeline build(IExpression expression) {
        return expression.accept(new Builder());
    }

    private class Builder implements IMetricExpressionVisitor<IPipeline> {
        @Override
        public IPipeline visit(MetricExpression expression) {
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

            // TODO: handling relative comparison

            return new MetricExpressionPipeline(req, dataSourceApi);
        }

        @Override
        public IPipeline visit(LiteralExpression<?> expression) {
            return new LiteralPipeline(expression);
        }

        @Override
        public IPipeline visit(ArithmeticExpression expression) {
            return switch (expression.getType()) {
                case "+" -> new BinaryExpressionPipeline.Add(expression.getLhs().accept(this), expression.getRhs().accept(this));
                case "-" -> new BinaryExpressionPipeline.Sub(expression.getLhs().accept(this), expression.getRhs().accept(this));
                case "*" -> new BinaryExpressionPipeline.Mul(expression.getLhs().accept(this), expression.getRhs().accept(this));
                case "/" -> new BinaryExpressionPipeline.Div(expression.getLhs().accept(this), expression.getRhs().accept(this));
                default -> throw new UnsupportedOperationException("Unsupported arithmetic expression: " + expression.getType());
            };
        }
    }
}
