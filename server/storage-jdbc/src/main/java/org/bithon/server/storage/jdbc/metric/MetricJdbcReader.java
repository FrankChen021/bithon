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

package org.bithon.server.storage.jdbc.metric;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.query.OrderBy;
import org.bithon.server.storage.datasource.query.Query;
import org.bithon.server.storage.datasource.query.ast.Column;
import org.bithon.server.storage.datasource.query.ast.SelectExpression;
import org.bithon.server.storage.datasource.query.ast.StringNode;
import org.bithon.server.storage.jdbc.utils.ISqlDialect;
import org.bithon.server.storage.jdbc.utils.SQLFilterBuilder;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.storage.metrics.IMetricReader;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:39 下午
 */
@Slf4j
public class MetricJdbcReader implements IMetricReader {

    private static final String TIMESTAMP_ALIAS_NAME = "_timestamp";

    protected final DSLContext dsl;
    protected final ISqlDialect sqlDialect;

    public MetricJdbcReader(DSLContext dsl, ISqlDialect sqlDialect) {
        this.dsl = dsl;
        this.sqlDialect = sqlDialect;
    }

    @Override
    public List<Map<String, Object>> timeseries(Query query) {
        SelectExpression selectExpression = SelectExpressionBuilder.builder()
                                                                   .dataSource(query.getDataSource())
                                                                   .fields(query.getResultColumns())
                                                                   .filters(query.getFilters())
                                                                   .interval(query.getInterval())
                                                                   .groupBys(query.getGroupBy())
                                                                   .orderBy(OrderBy.builder().name(TIMESTAMP_ALIAS_NAME).build())
                                                                   .sqlDialect(this.sqlDialect)
                                                                   .build();

        SelectExpression timestampFilterExpression = selectExpression;
        if (selectExpression.getFrom().getExpression() instanceof SelectExpression) {
            // Has a sub-query, timestampExpression will be put in sub-query
            timestampFilterExpression = (SelectExpression) selectExpression.getFrom().getExpression();

            // Add timestamp field to outer query at first position
            selectExpression.getResultColumnList().insert(new Column(TIMESTAMP_ALIAS_NAME));
        }

        // Add timestamp expression to sub-query
        timestampFilterExpression.getResultColumnList()
                                 .insert(new StringNode(StringUtils.format("%s AS \"%s\"",
                                                                           sqlDialect.timeFloorExpression("timestamp", query.getInterval().getStep()),
                                                                           TIMESTAMP_ALIAS_NAME)));

        selectExpression.getGroupBy().addField(TIMESTAMP_ALIAS_NAME);

        SqlGenerator sqlGenerator = new SqlGenerator();
        selectExpression.accept(sqlGenerator);
        return executeSql(sqlGenerator.getSQL());
    }

    @Override
    public List<?> groupBy(Query query) {
        SelectExpression selectExpression = SelectExpressionBuilder.builder()
                                                                   .dataSource(query.getDataSource())
                                                                   .fields(query.getResultColumns())
                                                                   .filters(query.getFilters())
                                                                   .interval(query.getInterval())
                                                                   .groupBys(query.getGroupBy())
                                                                   .orderBy(query.getOrderBy())
                                                                   .limit(query.getLimit())
                                                                   .sqlDialect(this.sqlDialect)
                                                                   .build();

        SqlGenerator sqlGenerator = new SqlGenerator();
        selectExpression.accept(sqlGenerator);
        return fetch(sqlGenerator.getSQL(), query.getResultFormat());
    }

    private String getOrderBySQL(OrderBy orderBy) {
        if (orderBy == null) {
            return "";
        }
        return "ORDER BY \"" + orderBy.getName() + "\" " + orderBy.getOrder();
    }

    @Override
    public List<Map<String, Object>> list(Query query) {
        String sqlTableName = "bithon_" + query.getDataSource().getName().replace("-", "_");

        String filter = SQLFilterBuilder.build(query.getDataSource(), query.getFilters());
        String sql = StringUtils.format(
            "SELECT %s FROM \"%s\" WHERE %s %s \"timestamp\" >= %s AND \"timestamp\" < %s %s LIMIT %d OFFSET %d",
            query.getResultColumns().stream().map(field -> "\"" + field.getColumnExpression() + "\"").collect(Collectors.joining(",")),
            sqlTableName,
            filter,
            StringUtils.hasText(filter) ? "AND" : "",
            sqlDialect.formatTimestamp(query.getInterval().getStartTime()),
            sqlDialect.formatTimestamp(query.getInterval().getEndTime()),
            getOrderBySQL(query.getOrderBy()),
            query.getLimit().getLimit(),
            query.getLimit().getOffset()
        );

        return executeSql(sql);
    }

    @Override
    public int listSize(Query query) {
        String sqlTableName = "bithon_" + query.getDataSource().getName().replace("-", "_");

        String filter = SQLFilterBuilder.build(query.getDataSource(), query.getFilters());
        String sql = StringUtils.format(
            "SELECT count(1) FROM \"%s\" WHERE %s %s \"timestamp\" >= %s AND \"timestamp\" < %s",
            sqlTableName,
            filter,
            StringUtils.hasText(filter) ? "AND" : "",
            sqlDialect.formatTimestamp(query.getInterval().getStartTime()),
            sqlDialect.formatTimestamp(query.getInterval().getEndTime())
        );

        Record record = dsl.fetchOne(sql);
        return ((Number) record.get(0)).intValue();
    }

    private List<?> fetch(String sql, Query.ResultFormat resultFormat) {
        log.info("Executing {}", sql);

        List<Record> records = dsl.fetch(sql);

        if (resultFormat == Query.ResultFormat.Object) {
            return records.stream().map(record -> {
                Map<String, Object> mapObject = new HashMap<>(record.fields().length);
                for (Field<?> field : record.fields()) {
                    mapObject.put(field.getName(), record.get(field));
                }
                return mapObject;
            }).collect(Collectors.toList());
        } else {
            return records.stream().map(Record::intoArray).collect(Collectors.toList());
        }
    }

    @Override
    public List<Map<String, Object>> executeSql(String sql) {
        log.info("Executing {}", sql);

        List<Record> records = dsl.fetch(sql);

        // PAY ATTENTION:
        //  although the explicit cast seems unnecessary, it must be kept so that compilation can pass
        //  this is might be a bug of JDK
        return (List<Map<String, Object>>) records.stream().map(record -> {
            Map<String, Object> mapObject = new HashMap<>(record.fields().length);
            for (Field<?> field : record.fields()) {
                mapObject.put(field.getName(), record.get(field));
            }
            return mapObject;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, String>> getDimensionValueList(TimeSpan start,
                                                           TimeSpan end,
                                                           DataSourceSchema dataSourceSchema,
                                                           Collection<IFilter> conditions,
                                                           String dimension) {
        String condition = conditions.stream()
                                     .map(d -> d.getMatcher().accept(new SQLFilterBuilder(dataSourceSchema, d)) + " AND ")
                                     .collect(Collectors.joining());

        String sql = StringUtils.format(
            "SELECT DISTINCT(\"%s\") \"%s\" FROM \"%s\" WHERE %s \"timestamp\" >= %s AND \"timestamp\" < %s AND \"%s\" IS NOT NULL ORDER BY \"%s\"",
            dimension,
            dimension,
            "bithon_" + dataSourceSchema.getName().replace("-", "_"),
            condition,
            sqlDialect.formatTimestamp(start),
            sqlDialect.formatTimestamp(end),
            dimension,
            dimension
        );

        log.info("Executing {}", sql);
        List<Record> records = dsl.fetch(sql);
        return records.stream().map(record -> {
            Field<?>[] fields = record.fields();
            Map<String, String> mapObject = new HashMap<>(fields.length);
            for (Field<?> field : fields) {
                mapObject.put("value", record.get(field).toString());
            }
            return mapObject;
        }).collect(Collectors.toList());
    }
}
