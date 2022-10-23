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

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.api.IQueryStageAggregator;
import org.bithon.server.storage.datasource.api.QueryStageAggregators;
import org.bithon.server.storage.datasource.spec.CountMetricSpec;
import org.bithon.server.storage.datasource.spec.IMetricSpec;
import org.bithon.server.storage.datasource.spec.IMetricSpecVisitor;
import org.bithon.server.storage.datasource.spec.PostAggregatorExpressionVisitor;
import org.bithon.server.storage.datasource.spec.PostAggregatorMetricSpec;
import org.bithon.server.storage.datasource.spec.gauge.GaugeMetricSpec;
import org.bithon.server.storage.datasource.spec.max.MaxMetricSpec;
import org.bithon.server.storage.datasource.spec.min.MinMetricSpec;
import org.bithon.server.storage.datasource.spec.sum.SumMetricSpec;
import org.bithon.server.storage.jdbc.dsl.sql.GroupByExpression;
import org.bithon.server.storage.jdbc.dsl.sql.NameExpression;
import org.bithon.server.storage.jdbc.dsl.sql.OrderByExpression;
import org.bithon.server.storage.jdbc.dsl.sql.SelectExpression;
import org.bithon.server.storage.jdbc.dsl.sql.StringExpression;
import org.bithon.server.storage.jdbc.dsl.sql.TableExpression;
import org.bithon.server.storage.jdbc.dsl.sql.WhereExpression;
import org.bithon.server.storage.jdbc.utils.SQLFilterBuilder;
import org.bithon.server.storage.metrics.GroupByQuery;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.storage.metrics.IMetricReader;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.storage.metrics.ListQuery;
import org.bithon.server.storage.metrics.OrderBy;
import org.bithon.server.storage.metrics.TimeseriesQueryV2;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:39 下午
 */
@Slf4j
public class MetricJdbcReader implements IMetricReader {

    private static final String TIMESTAMP_QUERY_NAME = "_timestamp";

    protected final DSLContext dsl;
    protected final ISqlExpressionFormatter sqlFormatter;

    public MetricJdbcReader(DSLContext dsl, ISqlExpressionFormatter sqlFormatter) {
        this.dsl = dsl;
        this.sqlFormatter = sqlFormatter;
    }

    @Override
    public List<Map<String, Object>> timeseries(TimeseriesQueryV2 query) {
        SelectExpression selectExpression = createSelectExpression(query.getDataSource(),
                                                                   query.getMetrics(),
                                                                   query.getAggregators(),
                                                                   query.getFilters(),
                                                                   query.getInterval(),
                                                                   query.getGroupBy(),
                                                                   OrderBy.builder().name(TIMESTAMP_QUERY_NAME).build());

        SelectExpression timestampExpressionOn = selectExpression;
        if (selectExpression.getFrom().getExpression() instanceof SelectExpression) {
            // Has a sub-query, timestampExpression will be put in sub-query
            timestampExpressionOn = (SelectExpression) selectExpression.getFrom().getExpression();

            // Add timestamp field to outer query
            selectExpression.getFieldsExpression().insert(new NameExpression("_timestamp"));
        }

        // Add timestamp expression to sub-query
        timestampExpressionOn.getFieldsExpression()
                             .insert(new StringExpression(StringUtils.format("%s AS \"_timestamp\"",
                                                                             sqlFormatter.timeFloor("timestamp", query.getInterval().getStep()))));

        selectExpression.getGroupBy().addField("_timestamp");

        SQLGenerator sqlGenerator = new SQLGenerator();
        selectExpression.accept(sqlGenerator);
        return executeSql(sqlGenerator.getSQL());
    }

    private SelectExpression createSelectExpression(DataSourceSchema dataSource,
                                                    List<String> metrics,
                                                    List<IQueryStageAggregator> aggregators,
                                                    Collection<IFilter> filters,
                                                    Interval interval,
                                                    List<String> groupBys,
                                                    OrderBy orderBy) {
        String sqlTableName = "bithon_" + dataSource.getName().replace("-", "_");

        List<IQueryStageAggregator> aggregatorList = new ArrayList<>(aggregators);
        if (CollectionUtils.isNotEmpty(metrics)) {
            // to compatible with old interface
            for (String metric : metrics) {
                IMetricSpec metricSpec = dataSource.getMetricSpecByName(metric);
                if (metricSpec == null) {
                    throw new RuntimeException(StringUtils.format("metric[%s] does not exist.", metric));
                }
                IQueryStageAggregator aggregator = metricSpec.getQueryAggregator();
                if (aggregator != null) {
                    aggregatorList.add(aggregator);
                }
            }
        }

        SelectExpression selectExpression = new SelectExpression();
        selectExpression.setGroupBy(new GroupByExpression());
        SelectExpression subSelectExpression = new SelectExpression();

        Set<String> aggregatorExpressions = new HashSet<>();

        //
        // fields
        //
        boolean hasSubSelect = false;
        QueryStageAggregatorSQLGenerator generator = new QueryStageAggregatorSQLGenerator(sqlFormatter,
                                                                                          interval.getTotalLength(),
                                                                                          interval.getStep());
        for (IQueryStageAggregator aggregator : aggregatorList) {
            // if window function is contained, the final SQL is different
            if (sqlFormatter.useWindowFunctionAsAggregator(aggregator)) {
                subSelectExpression.getFieldsExpression().addField(new StringExpression(aggregator.accept(generator)));

                selectExpression.getGroupBy().addField(aggregator.getName());
                selectExpression.getFieldsExpression().addField(new NameExpression(aggregator.getName()));

                hasSubSelect = true;
            } else {
                selectExpression.getFieldsExpression().addField(new StringExpression(aggregator.accept(generator)));

                aggregatorExpressions.add(StringUtils.format("%s(\"%s\")", aggregator.getType(), aggregator.getField()));
            }
        }

        // post aggregators
        if (!CollectionUtils.isEmpty(metrics)) {
            MetricFieldsClauseBuilder metricFieldsBuilder = this.createMetriClauseBuilder(sqlTableName,
                                                                                          dataSource,
                                                                                          aggregatorExpressions,
                                                                                          ImmutableMap.of("interval", interval.getStep(),
                                                                                                          "instanceCount", "count(distinct \"instanceName\")"));

            for (String metric : metrics) {
                IMetricSpec metricSpec = dataSource.getMetricSpecByName(metric);
                if (metricSpec instanceof PostAggregatorMetricSpec) {
                    selectExpression.getFieldsExpression().addField(new StringExpression(metricSpec.accept(metricFieldsBuilder)));
                }
            }
        }

        //
        // build WhereExpression
        //
        WhereExpression whereExpression = new WhereExpression();
        whereExpression.addExpression(StringUtils.format("\"timestamp\" >= %s", sqlFormatter.formatTimestamp(interval.getStartTime())));
        whereExpression.addExpression(StringUtils.format("\"timestamp\" < %s", sqlFormatter.formatTimestamp(interval.getEndTime())));
        for (IFilter filter : filters) {
            whereExpression.addExpression(filter.getMatcher().accept(new SQLFilterBuilder(dataSource, filter)));
        }

        //
        // build GroupByExpression
        //
        selectExpression.getFieldsExpression().addFields(groupBys);
        selectExpression.getGroupBy().addFields(groupBys);

        //
        // build OrderByExpression
        //
        if (orderBy != null) {
            selectExpression.setOrderBy(new OrderByExpression(orderBy.getName(), orderBy.getOrder()));
        }

        //
        // Link query and subQuery together
        //
        if (hasSubSelect) {
            subSelectExpression.getFrom().setExpression(new TableExpression(sqlTableName));
            subSelectExpression.setWhere(whereExpression);
            selectExpression.getFrom().setExpression(subSelectExpression);
        } else {
            selectExpression.getFrom().setExpression(new TableExpression(sqlTableName));
            selectExpression.setWhere(whereExpression);
        }
        return selectExpression;
    }

    @Override
    public List<Map<String, Object>> groupBy(GroupByQuery query) {
        SelectExpression selectExpression = createSelectExpression(query.getDataSource(),
                                                                   query.getMetrics(),
                                                                   query.getAggregators(),
                                                                   query.getFilters(),
                                                                   query.getInterval(),
                                                                   query.getGroupBys(),
                                                                   query.getOrderBy());

        SQLGenerator sqlGenerator = new SQLGenerator();
        selectExpression.accept(sqlGenerator);
        return executeSql(sqlGenerator.getSQL());
    }

    protected MetricFieldsClauseBuilder createMetriClauseBuilder(String tableName,
                                                                 DataSourceSchema dataSource,
                                                                 Set<String> existingAggregators,
                                                                 Map<String, Object> variables) {
        return new MetricFieldsClauseBuilder(this.sqlFormatter, tableName, "OUTER", existingAggregators, dataSource, variables);
    }

    private String getOrderBySQL(OrderBy orderBy) {
        if (orderBy == null) {
            return "";
        }
        return "ORDER BY \"" + orderBy.getName() + "\" " + orderBy.getOrder();
    }

    @Override
    public List<Map<String, Object>> list(ListQuery query) {
        String sqlTableName = "bithon_" + query.getSchema().getName().replace("-", "_");

        String filter = SQLFilterBuilder.build(query.getSchema(), query.getFilters());
        String sql = StringUtils.format(
            "SELECT %s FROM \"%s\" WHERE %s %s \"timestamp\" >= %s AND \"timestamp\" < %s %s LIMIT %d OFFSET %d",
            query.getColumns().stream().map(column -> "\"" + column + "\"").collect(Collectors.joining(",")),
            sqlTableName,
            filter,
            StringUtils.hasText(filter) ? "AND" : "",
            sqlFormatter.formatTimestamp(query.getInterval().getStartTime()),
            sqlFormatter.formatTimestamp(query.getInterval().getEndTime()),
            getOrderBySQL(query.getOrderBy()),
            query.getPageSize(),
            query.getPageNumber() * query.getPageSize()
        );

        return executeSql(sql);
    }

    @Override
    public int listSize(ListQuery query) {
        String sqlTableName = "bithon_" + query.getSchema().getName().replace("-", "_");

        String filter = SQLFilterBuilder.build(query.getSchema(), query.getFilters());
        String sql = StringUtils.format(
            "SELECT count(\"%s\") FROM \"%s\" WHERE %s %s \"timestamp\" >= %s AND \"timestamp\" < %s",
            query.getColumns().get(0),
            sqlTableName,
            filter,
            StringUtils.hasText(filter) ? "AND" : "",
            sqlFormatter.formatTimestamp(query.getInterval().getStartTime()),
            sqlFormatter.formatTimestamp(query.getInterval().getEndTime())
        );

        Record record = dsl.fetchOne(sql);
        return ((Number) record.get(0)).intValue();
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
            sqlFormatter.formatTimestamp(start),
            sqlFormatter.formatTimestamp(end),
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

    static class DefaultSqlExpressionFormatter implements ISqlExpressionFormatter {
        public static ISqlExpressionFormatter INSTANCE = new DefaultSqlExpressionFormatter();

        @Override
        public boolean groupByUseRawExpression() {
            return false;
        }

        @Override
        public boolean allowSameAggregatorExpression() {
            return true;
        }

        @Override
        public String stringAggregator(String field, String name) {
            throw new RuntimeException("string agg is not supported.");
        }

        @Override
        public String firstAggregator(String field, String name, long window) {
            throw new RuntimeException("last agg is not supported.");
        }

        @Override
        public String lastAggregator(String field, String name, long window) {
            throw new RuntimeException("last agg is not supported.");
        }
    }

    static class H2SqlExpressionFormatter implements ISqlExpressionFormatter {
        public static ISqlExpressionFormatter INSTANCE = new H2SqlExpressionFormatter();

        @Override
        public boolean groupByUseRawExpression() {
            return true;
        }

        @Override
        public boolean allowSameAggregatorExpression() {
            return true;
        }

        @Override
        public String stringAggregator(String field, String name) {
            return StringUtils.format("group_concat(\"%s\") AS \"%s\"", field, name);
        }

        @Override
        public String firstAggregator(String field, String name, long window) {
            return StringUtils.format(
                "FIRST_VALUE(\"%s\") OVER (partition by %s ORDER BY \"timestamp\") AS \"%s\"",
                field,
                this.timeFloor("timestamp", window),
                name);
        }

        @Override
        public String lastAggregator(String field, String name, long window) {
            // NOTE: use FIRST_VALUE
            return StringUtils.format(
                "FIRST_VALUE(\"%s\") OVER (partition by %s ORDER BY \"timestamp\" DESC) AS \"%s\"",
                field,
                this.timeFloor("timestamp", window),
                name);
        }

        @Override
        public boolean useWindowFunctionAsAggregator(IQueryStageAggregator aggregator) {
            return QueryStageAggregators.FirstAggregator.TYPE.equals(aggregator.getType())
                   || QueryStageAggregators.LastAggregator.TYPE.equals(aggregator.getType());
        }

        /*
         * NOTE, H2 does not support timestamp comparison, we have to use ISO8601 format
         */
    }

    /**
     * build SQL clause which aggregates specified metric
     */
    public static class MetricFieldsClauseBuilder implements IMetricSpecVisitor<String> {

        protected final String sqlTableName;
        protected final String tableAlias;
        protected final DataSourceSchema dataSource;
        protected final boolean addAlias;
        protected final Map<String, Object> variables;

        /**
         * used to keep which metrics current SQL are using
         */
        protected final Set<String> existingAggregators;
        protected final ISqlExpressionFormatter sqlExpressionFormatter;

        public MetricFieldsClauseBuilder(ISqlExpressionFormatter sqlExpressionFormatter,
                                         String sqlTableName,
                                         String tableAlias,
                                         Set<String> existingAggregators,
                                         DataSourceSchema dataSource,
                                         Map<String, Object> variables) {
            this(sqlExpressionFormatter, sqlTableName, tableAlias, existingAggregators, dataSource, variables, true);
        }

        public MetricFieldsClauseBuilder(ISqlExpressionFormatter sqlExpressionFormatter,
                                         String sqlTableName,
                                         String tableAlias,
                                         Set<String> existingAggregators,
                                         DataSourceSchema dataSource,
                                         Map<String, Object> variables,
                                         boolean addAlias) {
            this.sqlExpressionFormatter = sqlExpressionFormatter;
            this.sqlTableName = sqlTableName;
            this.tableAlias = tableAlias;
            this.dataSource = dataSource;
            this.variables = variables;
            this.addAlias = addAlias;
            this.existingAggregators = existingAggregators;
        }

        public MetricFieldsClauseBuilder clone(ISqlExpressionFormatter sqlExpressionFormatter,
                                               String sqlTableName,
                                               String tableAlias,
                                               DataSourceSchema dataSource,
                                               Map<String, Object> variables,
                                               boolean addAlias) {
            return new MetricFieldsClauseBuilder(sqlExpressionFormatter,
                                                 sqlTableName,
                                                 tableAlias,
                                                 new HashSet<>(),
                                                 dataSource,
                                                 variables,
                                                 addAlias);
        }

        @Override
        public String visit(SumMetricSpec metricSpec) {
            String expr = StringUtils.format("sum(\"%s\")", metricSpec.getName());
            existingAggregators.add(expr);

            StringBuilder sb = new StringBuilder();
            sb.append(expr);
            if (addAlias) {
                sb.append(StringUtils.format(" AS \"%s\"", metricSpec.getName()));
            }
            return sb.toString();
        }

        @Override
        public String visit(PostAggregatorMetricSpec metricSpec) {
            StringBuilder sb = new StringBuilder();
            metricSpec.visitExpression(new PostAggregatorExpressionVisitor() {
                @Override
                public void visitMetric(IMetricSpec metricSpec) {
                    String expr = metricSpec.accept(MetricFieldsClauseBuilder.this.clone(sqlExpressionFormatter,
                                                                                         null,
                                                                                         null,
                                                                                         dataSource,
                                                                                         variables,
                                                                                         false));

                    if (!sqlExpressionFormatter.allowSameAggregatorExpression()
                        && existingAggregators.contains(expr)) {
                        // this metricSpec has been in the SQL,
                        // don't need to construct the aggregation expression for this post aggregator
                        // because some DBMS does not support duplicated aggregation expressions for one metric
                        sb.append(metricSpec.getName());
                    } else {
                        sb.append(expr);
                    }
                }

                @Override
                public void visitNumber(String number) {
                    sb.append(number);
                }

                @Override
                public void visitorOperator(String operator) {
                    sb.append(operator);
                }

                @Override
                public void startBrace() {
                    sb.append('(');
                }

                @Override
                public void endBrace() {
                    sb.append(')');
                }

                @Override
                public void visitVariable(String variable) {
                    Object variableValue = variables.get(variable);
                    if (variableValue == null) {
                        throw new RuntimeException(StringUtils.format("variable (%s) not provided in context",
                                                                      variable));
                    }
                    sb.append(variableValue);
                }
            });
            sb.append(StringUtils.format(" \"%s\"", metricSpec.getName()));
            return sb.toString();
        }

        @Override
        public String visit(CountMetricSpec metricSpec) {
            String expr = StringUtils.format("count(1)", metricSpec.getName());
            existingAggregators.add(expr);

            StringBuilder sb = new StringBuilder();
            sb.append(expr);
            if (addAlias) {
                sb.append(StringUtils.format("count(1) AS \"%s\"", metricSpec.getName()));
            }
            return sb.toString();
        }

        /**
         * Since FIRST/LAST aggregators are not supported in many SQL databases,
         * A embedded query is created to simulate FIRST/LAST
         */
        @Override
        public String visit(GaugeMetricSpec metricSpec) {
            return visitLast(metricSpec.getName());
        }

        @Override
        public String visit(MinMetricSpec metricSpec) {
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.format("min(\"%s\")", metricSpec.getName()));
            if (addAlias) {
                sb.append(StringUtils.format(" \"%s\"", metricSpec.getName()));
            }
            return sb.toString();
        }

        @Override
        public String visit(MaxMetricSpec metricSpec) {
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.format("max(\"%s\")", metricSpec.getName()));
            if (addAlias) {
                sb.append(StringUtils.format(" \"%s\"", metricSpec.getName()));
            }
            return sb.toString();
        }

        protected String visitLast(String metricName) {
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.format(
                "(SELECT \"%s\" FROM \"%s\" B WHERE B.\"timestamp\" = \"%s\".\"timestamp\" ORDER BY \"timestamp\" DESC LIMIT 1)",
                metricName,
                sqlTableName,
                tableAlias));

            if (addAlias) {
                sb.append(' ');
                sb.append('"');
                sb.append(metricName);
                sb.append('"');
            }
            return sb.toString();
        }
    }
}
