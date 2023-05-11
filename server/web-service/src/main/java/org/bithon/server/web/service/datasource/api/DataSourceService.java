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

package org.bithon.server.web.service.datasource.api;

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.IColumnSpec;
import org.bithon.server.storage.datasource.dimension.IDimensionSpec;
import org.bithon.server.storage.datasource.query.Query;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.ResultColumn;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpressions;
import org.bithon.server.storage.datasource.spec.IMetricSpec;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.common.bucket.TimeBucket;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 8/1/22 9:57 PM
 */
@Service
@Conditional(WebServiceModuleEnabler.class)
public class DataSourceService {

    private static final String TIMESTAMP_COLUMN_NAME_IN_RESULT_SET = "_timestamp";
    private final IMetricStorage metricStorage;

    public DataSourceService(IMetricStorage metricStorage) {
        this.metricStorage = metricStorage;
    }

    /**
     * @return Map<Tags, Val>
     * Tags - dimension of a series
     * Val - an array of all data points. Each element represents a data point of a timestamp.
     */
    public TimeSeriesQueryResult timeseriesQuery(Query query) {
        // Remove any dimensions
        List<String> metrics = query.getResultColumns()
                                    .stream()
                                    .filter((resultColumn) -> query.getDataSource()
                                                                   .getDimensionSpecByName(resultColumn.getResultColumnName())
                                                              == null)
                                    .map((ResultColumn::getResultColumnName))
                                    .collect(Collectors.toList());

        List<Map<String, Object>> points = this.metricStorage.createMetricReader(query.getDataSource())
                                                             .timeseries(query);

        return TimeSeriesQueryResult.build(query.getInterval().getStartTime(),
                                           query.getInterval().getEndTime(),
                                           query.getInterval().getStep(),
                                           points,
                                           TIMESTAMP_COLUMN_NAME_IN_RESULT_SET,
                                           query.getGroupBy(),
                                           metrics);
    }

    public Query convertToQuery(DataSourceSchema schema,
                                GeneralQueryRequest query,
                                boolean bucketTimestamp) {
        Query.QueryBuilder builder = Query.builder();

        List<String> groupBy = new ArrayList<>(4);

        // Turn into internal objects(post aggregators...)
        List<ResultColumn> resultColumnList = new ArrayList<>(query.getFields().size());
        for (QueryField field : query.getFields()) {
            if (field.getExpression() != null) {

                resultColumnList.add(new ResultColumn(new Expression(field.getExpression()), field.getName()));

                continue;
            }

            if (field.getAggregator() != null) {
                org.bithon.server.storage.datasource.query.ast.Function function = SimpleAggregateExpressions.create(
                    field.getAggregator(),
                    field.getField() == null
                    ? field.getName()
                    : field.getField());
                resultColumnList.add(new ResultColumn(function, field.getName()));
            } else {
                IColumnSpec columnSpec = schema.getColumnByName(field.getField());
                if (columnSpec == null) {
                    throw new RuntimeException(StringUtils.format("field [%s] does not exist.", field.getField()));
                }
                if (columnSpec instanceof IDimensionSpec) {
                    resultColumnList.add(new ResultColumn(columnSpec.getName()));
                    groupBy.add(columnSpec.getName());
                } else {
                    resultColumnList.add(((IMetricSpec) columnSpec).getResultColumn());
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
        }

        return builder.groupBy(new ArrayList<>(groupBy))
                      .resultColumns(resultColumnList)
                      .dataSource(schema)
                      .filters(FilterExpressionToFilters.toFilter(schema, query.getFilterExpression(), query.getFilters()))
                      .interval(Interval.of(start, end, step))
                      .orderBy(query.getOrderBy())
                      .limit(query.getLimit())
                      .resultFormat(query.getResultFormat() == null
                                    ? Query.ResultFormat.Object
                                    : query.getResultFormat())
                      .build();
    }

}
