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
import org.bithon.component.commons.expression.IDataType;
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
import org.bithon.server.web.service.datasource.api.QueryField;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/2/1 20:35
 */
public class QueryConverter {
    public static Query toQuery(ISchema schema,
                                QueryRequest query,
                                boolean useInputGroupBy,
                                boolean groupByTimestamp) {

        validateQueryRequest(schema, query);

        Query.QueryBuilder builder = Query.builder();

        List<String> groupBy;
        if (useInputGroupBy) {
            groupBy = CollectionUtils.emptyOrOriginal(query.getGroupBy());
        } else {
            groupBy = new ArrayList<>(4);
        }

        // Turn into internal objects (post aggregators...)
        List<Selector> selectorList = new ArrayList<>(query.getFields().size());
        for (QueryField field : query.getFields()) {
            if (field.getExpression() != null) {

                selectorList.add(new Selector(new Expression(field.getExpression()), field.getName()));

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
                        Preconditions.checkNotNull(schema.getColumnByName(field.getField()), "Field [%s] does not exist in the schema.", field.getField());
                        selectorList.add(new Selector(new Expression(new FunctionExpression(function, new IdentifierExpression(field.getField()))), field.getName()));
                    }
                } else {
                    String col = field.getField() == null ? field.getName() : field.getField();
                    Preconditions.checkNotNull(schema.getColumnByName(col), "Field [%s] does not exist in the schema.", col);
                    selectorList.add(new Selector(new Expression(new FunctionExpression(function, new IdentifierExpression(col))), field.getName()));
                }

            } else {
                IColumn columnSpec = schema.getColumnByName(field.getField());
                Preconditions.checkNotNull(columnSpec, "Field [%s] does not exist in the schema.", field.getField());

                Selector selector = columnSpec.toSelector();
                if (columnSpec.getAlias().equals(field.getName())) {
                    selector = selector.withOutput(field.getName());
                }
                selectorList.add(selector);

                if (!useInputGroupBy && columnSpec.getDataType().equals(IDataType.STRING)) {
                    groupBy.add(field.getName());
                }
            }
        }

        TimeSpan start = TimeSpan.fromISO8601(query.getInterval().getStartISO8601());
        TimeSpan end = TimeSpan.fromISO8601(query.getInterval().getEndISO8601());

        Duration step = null;
        if (groupByTimestamp) {
            if (query.getInterval().getBucketCount() == null) {
                step = Duration.ofSeconds(TimeBucket.calculate(start, end));
            } else {
                step = Duration.ofSeconds(TimeBucket.calculate(start.getMilliseconds(),
                                                               end.getMilliseconds(),
                                                               query.getInterval().getBucketCount()).getLength());
            }
            if (query.getInterval().getMinBucketLength() != null) {
                int minStep = query.getInterval().getMinBucketLength();
                if (minStep > step.getSeconds()) {
                    step = Duration.ofSeconds(minStep);
                }
            }
        }

        String timestampColumn = schema.getTimestampSpec().getColumnName();
        if (StringUtils.hasText(query.getInterval().getTimestampColumn())) {
            // Try to use query's timestamp column if provided
            timestampColumn = query.getInterval().getTimestampColumn();
        }

        return builder.groupBy(new ArrayList<>(groupBy))
                      .selectors(selectorList)
                      .schema(schema)
                      .filter(QueryFilter.build(schema, query.getFilterExpression()))
                      .interval(Interval.of(start, end, step, new IdentifierExpression(timestampColumn)))
                      .orderBy(query.getOrderBy())
                      .limit(query.getLimit())
                      .resultFormat(query.getResultFormat() == null
                                        ? Query.ResultFormat.Object
                                        : query.getResultFormat())
                      .build();
    }

    public static Query toSelectQuery(ISchema schema,
                                      QueryRequest query) {

        validateQueryRequest(schema, query);
        Preconditions.checkIfTrue(CollectionUtils.isEmpty(query.getGroupBy()), "Select query should not come with the 'groupBy' property.");
        Preconditions.checkNotNull(query.getLimit(), "Select query must come with the 'limit' property");

        Query.QueryBuilder builder = Query.builder();

        List<Selector> selectorList = new ArrayList<>(query.getFields().size());
        for (QueryField field : query.getFields()) {
            if (field.getExpression() != null) {
                // TODO: check if there's any aggregation function in the expression
                selectorList.add(new Selector(new Expression(field.getExpression()), field.getName()));

                continue;
            }

            if (field.getAggregator() != null) {
                throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                                "Aggregator [%s] on field [%s] is not allowed in select query",
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
                selector = new Selector(columnSpec.getName());
            }
            if (columnSpec.getAlias().equals(field.getName())) {
                selector = selector.withOutput(field.getName());
            }
            selectorList.add(selector);
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
                      .resultFormat(query.getResultFormat() == null
                                        ? Query.ResultFormat.Object
                                        : query.getResultFormat())
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
                                    .anyMatch((filter) -> filter.getName().equals(orderBy.getName()));

            Preconditions.checkIfTrue(exists, "OrderBy field [%s] can not be found in the query fields.", orderBy.getName());
        }
    }
}
