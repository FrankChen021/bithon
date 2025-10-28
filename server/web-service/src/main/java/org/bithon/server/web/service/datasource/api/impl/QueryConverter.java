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

package org.bithon.server.web.service.datasource.api.impl;

import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.expression.validation.ExpressionValidationException;
import org.bithon.component.commons.expression.validation.IIdentifier;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.column.ExpressionColumn;
import org.bithon.server.datasource.column.IColumn;
import org.bithon.server.datasource.expression.ExpressionASTBuilder;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.OrderBy;
import org.bithon.server.datasource.query.Query;
import org.bithon.server.datasource.query.ResultFormat;
import org.bithon.server.datasource.query.ast.ExpressionNode;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryField;
import org.bithon.server.web.service.datasource.api.QueryRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/2/1 20:35
 */
public class QueryConverter {
    public static Query toQuery(ISchema schema, QueryRequest request) {
        Integer step = request.getInterval().getStep();
        return toQuery(schema,
                       request,
                       step == null ? null : Duration.ofSeconds(step));
    }

    public static Query toQuery(ISchema schema,
                                QueryRequest query,
                                Duration step) {
        validate(schema, query);

        boolean hasAggregator = false;

        List<Selector> selectorList = new ArrayList<>(query.getFields().size());
        int insertedIndex = -1;
        for (QueryField field : query.getFields()) {
            if (LiteralExpression.AsteriskLiteral.INSTANCE.getValue().equals(field.getField())) {
                // select all fields - handle asterisk for both aggregation and select queries
                insertedIndex = selectorList.size();
                continue;
            }

            if (field.getExpression() != null) {
                IExpression parsedExpression = ExpressionASTBuilder.builder()
                                                                   .functions(Functions.getInstance())
                                                                   .schema(schema)
                                                                   .build(field.getExpression());
                if (!hasAggregator) {
                    if (new AggregatorFinder().find(parsedExpression)) {
                        hasAggregator = true;
                    }
                }
                selectorList.add(new Selector(new ExpressionNode(parsedExpression), field.getName()));
                continue;
            }

            if (field.getAggregator() != null) {
                hasAggregator = true;

                IFunction function = Functions.getInstance().getFunction(field.getAggregator());

                if (function instanceof AggregateFunction.Count) {
                    // Count aggregation has special input like count(), count(*), count(1),
                    // we need to treat them differently
                    if (StringUtils.isEmpty(field.getField())) {
                        selectorList.add(new Selector(new ExpressionNode(new FunctionExpression(function, LiteralExpression.ofLong(1L))), field.getName()));
                    } else if ("*".equals(field.getField())) {
                        selectorList.add(new Selector(new ExpressionNode(new FunctionExpression(function, LiteralExpression.AsteriskLiteral.INSTANCE)), field.getName()));
                    } else if (field.getField().matches("\\d+")) {
                        selectorList.add(new Selector(new ExpressionNode(new FunctionExpression(function, LiteralExpression.ofLong(field.getField()))), field.getName()));
                    } else {
                        // Treat the input as a column name
                        IColumn column = schema.getColumnByName(field.getField());
                        Preconditions.checkNotNull(column, "Column [%s] does not exist in the schema.", field.getField());

                        if (column instanceof ExpressionColumn) {
                            // Count on a built-in expression column
                            selectorList.add(new Selector(new ExpressionNode(new FunctionExpression(function, LiteralExpression.AsteriskLiteral.INSTANCE)), field.getName()));
                        } else {
                            selectorList.add(new Selector(new ExpressionNode(column.createAggregateFunctionExpression(function)), field.getName()));
                        }
                    }
                } else {
                    String columnName = field.getField() == null ? field.getName() : field.getField();
                    IColumn column = schema.getColumnByName(columnName);
                    Preconditions.checkNotNull(column, "Column [%s] does not exist in the schema.", columnName);

                    selectorList.add(new Selector(new ExpressionNode(column.createAggregateFunctionExpression(function)), field.getName()));
                }
            } else {
                IColumn columnSpec = schema.getColumnByName(field.getField());
                Preconditions.checkNotNull(columnSpec, "Field [%s] does not exist in the schema.", field.getField());

                Selector selector;
                if (columnSpec instanceof ExpressionColumn) {
                    selector = columnSpec.toSelector();
                } else {
                    selector = new Selector(columnSpec.getName(), field.getName(), columnSpec.getDataType());
                }
                if (columnSpec.getAlias().equals(field.getName())) {
                    selector = selector.withOutput(field.getName());
                }
                selectorList.add(selector);
            }
        }

        // Replace the input '*' with all columns in the schema
        // the insertIndex is the position of the '*' in the selector list
        if (insertedIndex != -1) {
            Set<String> selectedColumns = selectorList.stream().map((Selector::getOutputName)).collect(Collectors.toSet());
            for (IColumn column : schema.getColumns()) {
                // Don't add expression column which usually contains aggregation function
                // This is to avoid aggregate function to be used on list query.
                // Maybe the check is not perfect, but it should be good enough
                if (!(column instanceof ExpressionColumn) && !selectedColumns.contains(column.getName())) {
                    // Create a new selector instance instead of call toSelector on column
                    // because the column may be column like LongLastColumn
                    selectorList.add(insertedIndex++, new Selector(column.getName(), column.getDataType()));
                }
            }
        }

        IntervalRequest interval = query.getInterval();
        String timestampColumn = schema.getTimestampSpec().getColumnName();
        if (StringUtils.hasText(interval.getTimestampColumn())) {
            // Try to use query's timestamp column if provided
            timestampColumn = interval.getTimestampColumn();
        }

        IExpression parsedFilterException = null;
        if (StringUtils.hasText(query.getFilterExpression())) {
            Map<String, Selector> outputNames = selectorList.stream()
                                                            .collect(Collectors.toMap(Selector::getOutputName, v -> v));

            parsedFilterException = QueryFilter.build(identifier -> {
                IColumn col = schema.getColumnByName(identifier);
                if (col != null) {
                    return col;
                }
                Selector outputColumn = outputNames.get(identifier);
                if (outputColumn != null) {
                    return new IIdentifier() {
                        @Override
                        public String getName() {
                            return identifier;
                        }

                        @Override
                        public IDataType getDataType() {
                            return outputColumn.getDataType();
                        }
                    };
                }
                throw new ExpressionValidationException("identifier [%s] not found in schema or in the output", identifier);
            }, query.getFilterExpression());
        }

        return Query.builder()
                    .schema(schema)
                    .isAggregateQuery(hasAggregator || interval.getStep() != null || CollectionUtils.isNotEmpty(query.getGroupBy()))
                    .selectors(selectorList)
                    .groupBy(new ArrayList<>(CollectionUtils.emptyOrOriginal(query.getGroupBy())))
                    .filter(parsedFilterException)
                    .interval(Interval.of(interval.getStartISO8601(),
                                          interval.getEndISO8601(),
                                          step, interval.getWindow(),
                                          new IdentifierExpression(timestampColumn)))
                    .orderBy(query.getOrderBy())
                    .limit(query.getLimit())
                    .offset(query.getOffset())
                    .settings(query.getSettings())
                    .resultFormat(query.getResultFormat() == null ? ResultFormat.Object : query.getResultFormat())
                    .build();
    }

    /**
     * Validate the request to ensure the safety
     */
    private static void validate(ISchema schema, QueryRequest request) {
        IntervalRequest interval = request.getInterval();
        if (interval.getWindow() != null && interval.getStep() != null) {
            Preconditions.checkIfTrue(interval.getWindow().getDuration().getSeconds() >= interval.getStep(),
                                      "The window parameter (%s) in the request is less than the step parameter (%d).",
                                      interval.getWindow(),
                                      interval.getStep());
        }

        if (CollectionUtils.isNotEmpty(request.getGroupBy())) {
            for (String field : request.getGroupBy()) {
                Preconditions.checkNotNull(schema.getColumnByName(field),
                                           "GroupBy field [%s] does not exist in the schema.",
                                           field);
            }
        }

        // If orderBy is given, make sure the order by fields are in the query fields
        // We don't check if the query fields are defined in the schema here,
        // they will be checked in other places
        OrderBy orderBy = request.getOrderBy();
        if (orderBy != null && StringUtils.hasText(orderBy.getName())) {
            boolean exists = request.getFields()
                                    .stream()
                                    .anyMatch((filter) -> filter.getName().equals(orderBy.getName()))
                             || (request.getGroupBy() != null && request.getGroupBy().contains(orderBy.getName()));

            boolean hasAll = request.getFields()
                                    .stream()
                                    .anyMatch((field) -> "*".equals(field.getField()));

            Preconditions.checkIfTrue(exists || hasAll, "OrderBy field [%s] can not be found in the query fields.", orderBy.getName());
        }
    }

    private static class AggregatorFinder implements IExpressionInDepthVisitor {
        private boolean hasAggregator;

        @Override
        public boolean visit(FunctionExpression expression) {
            if (expression.getFunction().isAggregator()) {
                hasAggregator = true;
            }

            // If aggregator not found, continue to visit
            return !hasAggregator;
        }

        public boolean find(IExpression expression) {
            hasAggregator = false;
            expression.accept(this);
            return hasAggregator;
        }
    }
}
