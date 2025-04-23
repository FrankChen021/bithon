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

import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.ExpressionColumn;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.query.OrderBy;
import org.bithon.server.storage.datasource.query.Query;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.Selector;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.web.service.common.bucket.TimeBucket;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryField;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/2/1 20:35
 */
public class QueryConverter {
    public static Query toQuery(ISchema schema,
                                QueryRequest query,
                                boolean groupByTimestamp) {
        IntervalRequest interval = query.getInterval();
        if (interval.getWindow() != null && interval.getStep() != null) {
            Preconditions.checkIfTrue(interval.getWindow().getDuration().getSeconds() >= interval.getStep(),
                                      "The window parameter (%s) in the request is less than the step parameter (%d).",
                                      interval.getWindow(),
                                      interval.getStep());
        }
        validateQueryRequest(schema, query);

        Query.QueryBuilder builder = Query.builder();

        Set<String> groupBy = CollectionUtils.emptyOrOriginal(query.getGroupBy());

        // Turn into internal objects (post aggregators...)
        List<Selector> selectorList = new ArrayList<>(query.getFields().size());
        for (QueryField field : query.getFields()) {
            if (field.getExpression() != null) {
                selectorList.add(new Selector(new Expression(schema, field.getExpression()), field.getName()));
                continue;
            }

            if (field.getAggregator() != null) {
                IFunction function = Functions.getInstance().getFunction(field.getAggregator());

                if (function instanceof AggregateFunction.Count) {
                    // Count aggregation has special input like count(), count(*), count(1),
                    // we need to treat them differently
                    if (StringUtils.isEmpty(field.getField())) {
                        selectorList.add(new Selector(new Expression(new FunctionExpression(function, LiteralExpression.ofLong(1L))), field.getName()));
                    } else if ("*".equals(field.getField())) {
                        selectorList.add(new Selector(new Expression(new FunctionExpression(function, LiteralExpression.AsteriskLiteral.INSTANCE)), field.getName()));
                    } else if (field.getField().matches("\\d+")) {
                        selectorList.add(new Selector(new Expression(new FunctionExpression(function, LiteralExpression.ofLong(field.getField()))), field.getName()));
                    } else {
                        // Treat the input as a column name
                        IColumn column = schema.getColumnByName(field.getField());
                        Preconditions.checkNotNull(column, "Column [%s] does not exist in the schema.", field.getField());

                        if (column instanceof ExpressionColumn) {
                            // Count on a built-in expression column
                            selectorList.add(new Selector(new Expression(new FunctionExpression(function, LiteralExpression.AsteriskLiteral.INSTANCE)), field.getName()));
                        } else {
                            selectorList.add(new Selector(new Expression(column.createAggregateFunctionExpression(function)), field.getName()));
                        }
                    }
                } else {
                    String columnName = field.getField() == null ? field.getName() : field.getField();
                    IColumn column = schema.getColumnByName(columnName);
                    Preconditions.checkNotNull(column, "Column [%s] does not exist in the schema.", columnName);

                    selectorList.add(new Selector(new Expression(column.createAggregateFunctionExpression(function)), field.getName()));
                }
            } else {
                IColumn columnSpec = schema.getColumnByName(field.getField());
                Preconditions.checkNotNull(columnSpec, "Column [%s] does not exist in the schema.", field.getField());

                Selector selector = columnSpec.toSelector();
                if (columnSpec.getAlias().equals(field.getName())) {
                    selector = selector.withOutput(field.getName());
                }
                selectorList.add(selector);
            }
        }

        TimeSpan start = TimeSpan.fromISO8601(interval.getStartISO8601());
        TimeSpan end = TimeSpan.fromISO8601(interval.getEndISO8601());

        Duration step = null;
        if (groupByTimestamp) {
            if (interval.getBucketCount() == null) {
                step = Duration.ofSeconds(TimeBucket.calculate(start, end));
            } else {
                step = Duration.ofSeconds(TimeBucket.calculate(start.getMilliseconds(),
                                                               end.getMilliseconds(),
                                                               interval.getBucketCount()).getLength());
            }

            if (interval.getStep() != null) {
                Preconditions.checkIfTrue(interval.getStep() > 0, "step must be greater than 0");
                step = Duration.ofSeconds(interval.getStep());
            }
        }

        String timestampColumn = schema.getTimestampSpec().getColumnName();
        if (StringUtils.hasText(interval.getTimestampColumn())) {
            // Try to use query's timestamp column if provided
            timestampColumn = interval.getTimestampColumn();
        }

        return builder.groupBy(new ArrayList<>(groupBy))
                      .selectors(selectorList)
                      .schema(schema)
                      .filter(QueryFilter.build(schema, query.getFilterExpression()))
                      .interval(Interval.of(start, end, step, interval.getWindow(), new IdentifierExpression(timestampColumn)))
                      .orderBy(query.getOrderBy())
                      .limit(query.getLimit())
                      .offset(query.getOffset())
                      .resultFormat(query.getResultFormat() == null ? Query.ResultFormat.Object : query.getResultFormat())
                      .build();
    }

    public static Query toSelectQuery(ISchema schema,
                                      QueryRequest query) {

        validateQueryRequest(schema, query);
        Preconditions.checkIfTrue(CollectionUtils.isEmpty(query.getGroupBy()), "Select query should not come with the 'groupBy' property.");
        Preconditions.checkNotNull(query.getLimit(), "Select query must come with the 'limit' property");

        Query.QueryBuilder builder = Query.builder();

        List<Selector> selectorList = new ArrayList<>(query.getFields().size());
        int insertedIndex = -1;
        for (QueryField field : query.getFields()) {
            if (LiteralExpression.AsteriskLiteral.INSTANCE.getValue().equals(field.getField())) {
                // select all fields
                insertedIndex = selectorList.size();
                continue;
            }

            if (field.getExpression() != null) {
                // This is a client side passed post simple expression, NOT aggregation expression
                // TODO: check if there's any aggregation function in the expression
                selectorList.add(new Selector(new Expression(schema, field.getExpression()), field.getName()));

                continue;
            }

            if (field.getAggregator() != null) {
                throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                                "Aggregator [%s] on field [%s] is not supported in a list query",
                                                field.getAggregator(),
                                                field.getName());
            }
            IColumn columnSpec = schema.getColumnByName(field.getField());
            Preconditions.checkNotNull(columnSpec, "Field [%s] does not exist in the schema.", field.getField());

            Selector selector;
            if (columnSpec instanceof ExpressionColumn) {
                selector = columnSpec.toSelector();
                // TODO: check if there's any aggregation function in the expression
            } else {
                selector = new Selector(columnSpec.getName(), columnSpec.getDataType());
            }
            if (columnSpec.getAlias().equals(field.getName())) {
                selector = selector.withOutput(field.getName());
            }
            selectorList.add(selector);
        }

        // Replace the input '*' with all columns in the schema
        // the insertIndex is the position of the '*' in the selector list
        if (insertedIndex != -1) {
            Set<String> selectedColumns = selectorList.stream().map((Selector::getOutputName)).collect(Collectors.toSet());
            for (IColumn column : schema.getColumns()) {
                if (!selectedColumns.contains(column.getName())) {
                    // Create a new selector instance instead of call toSelector on column
                    // because the column may be column like LongLastColumn
                    selectorList.add(insertedIndex++, new Selector(column.getName(), column.getDataType()));
                }
            }
        }

        TimeSpan start = TimeSpan.fromISO8601(query.getInterval().getStartISO8601());
        TimeSpan end = TimeSpan.fromISO8601(query.getInterval().getEndISO8601());

        String timestampColumn = schema.getTimestampSpec().getColumnName();
        if (StringUtils.hasText(query.getInterval().getTimestampColumn())) {
            // Try to use query's timestamp column if provided
            timestampColumn = query.getInterval().getTimestampColumn();
        }

        return builder.selectors(selectorList)
                      .schema(schema)
                      .filter(QueryFilter.build(schema, query.getFilterExpression()))
                      .interval(Interval.of(start, end, null, new IdentifierExpression(timestampColumn)))
                      .orderBy(query.getOrderBy())
                      .limit(query.getLimit())
                      .resultFormat(query.getResultFormat() == null ? Query.ResultFormat.Object : query.getResultFormat())
                      .build();
    }

    /**
     * Validate the request to ensure the safety
     */
    private static void validateQueryRequest(ISchema schema, QueryRequest request) {
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

            boolean hasAll = request.getFields().stream().anyMatch((field) -> "*".equals(field.getField()));

            Preconditions.checkIfTrue(exists || hasAll, "OrderBy field [%s] can not be found in the query fields.", orderBy.getName());
        }
    }
}
