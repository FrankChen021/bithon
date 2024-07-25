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

import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.common.expression.ExpressionASTBuilder;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.query.Query;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.QueryAggregateFunctions;
import org.bithon.server.storage.datasource.query.ast.SelectColumn;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.web.service.common.bucket.TimeBucket;
import org.bithon.server.web.service.datasource.api.GeneralQueryRequest;
import org.bithon.server.web.service.datasource.api.QueryField;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/2/1 20:35
 */
public class QueryBuilder {
    public static Query build(ISchema schema,
                              GeneralQueryRequest query,
                              boolean containsGroupBy,
                              boolean bucketTimestamp) {

        Query.QueryBuilder builder = Query.builder();

        List<String> groupBy;
        if (containsGroupBy) {
            groupBy = CollectionUtils.emptyOrOriginal(query.getGroupBy());
        } else {
            groupBy = new ArrayList<>(4);
        }

        // Turn into internal objects (post aggregators...)
        List<SelectColumn> selectColumnList = new ArrayList<>(query.getFields().size());
        for (QueryField field : query.getFields()) {
            if (field.getExpression() != null) {

                selectColumnList.add(new SelectColumn(new Expression(field.getExpression()), field.getName()));

                continue;
            }

            if (field.getAggregator() != null) {
                org.bithon.server.storage.datasource.query.ast.Function function = QueryAggregateFunctions.create(
                    field.getAggregator(),
                    field.getField() == null ? field.getName() : field.getField());
                selectColumnList.add(new SelectColumn(function, field.getName()));
            } else {
                IColumn columnSpec = schema.getColumnByName(field.getField());
                Preconditions.checkNotNull(columnSpec, "Field [%s] does not exist in the schema.", field.getField());

                SelectColumn selectColumn = columnSpec.toSelectColumn();
                if (columnSpec.getAlias().equals(field.getName())) {
                    selectColumn = selectColumn.withAlias(field.getName());
                }
                selectColumnList.add(selectColumn);

                if (!containsGroupBy && columnSpec.getDataType().equals(IDataType.STRING)) {
                    groupBy.add(field.getName());
                }
            }
        }

        TimeSpan start = TimeSpan.fromISO8601(query.getInterval().getStartISO8601());
        TimeSpan end = TimeSpan.fromISO8601(query.getInterval().getEndISO8601());

        Integer step = null;
        if (bucketTimestamp) {
            if (query.getInterval().getBucketCount() == null) {
                step = TimeBucket.calculate(start, end);
            } else {
                step = TimeBucket.calculate(start.getMilliseconds(),
                                            end.getMilliseconds(),
                                            query.getInterval().getBucketCount()).getLength();
            }
            if (query.getInterval().getMinBucketLength() != null) {
                step = Math.max(step, query.getInterval().getMinBucketLength());
            }
        }

        String timestampColumn = schema.getTimestampSpec().getColumnName();
        if (StringUtils.hasText(query.getInterval().getTimestampColumn())) {
            timestampColumn = query.getInterval().getTimestampColumn();
        }

        return builder.groupBy(new ArrayList<>(groupBy))
                      .selectColumns(selectColumnList)
                      .schema(schema)
                      .filter(QueryFilter.build(schema, query.getFilterExpression()))
                      .interval(Interval.of(start, end, step, ExpressionASTBuilder.builder().functions(Functions.getInstance()).build(timestampColumn)))
                      .orderBy(query.getOrderBy())
                      .limit(query.getLimit())
                      .resultFormat(query.getResultFormat() == null
                                        ? Query.ResultFormat.Object
                                        : query.getResultFormat())
                      .build();
    }
}
