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
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.common.utils.datetime.TimeSpan;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.aggregator.spec.CountMetricSpec;
import org.bithon.server.metric.aggregator.spec.DoubleLastMetricSpec;
import org.bithon.server.metric.aggregator.spec.DoubleSumMetricSpec;
import org.bithon.server.metric.aggregator.spec.IMetricSpec;
import org.bithon.server.metric.aggregator.spec.IMetricSpecVisitor;
import org.bithon.server.metric.aggregator.spec.LongLastMetricSpec;
import org.bithon.server.metric.aggregator.spec.LongMaxMetricSpec;
import org.bithon.server.metric.aggregator.spec.LongMinMetricSpec;
import org.bithon.server.metric.aggregator.spec.LongSumMetricSpec;
import org.bithon.server.metric.aggregator.spec.PostAggregatorExpressionVisitor;
import org.bithon.server.metric.aggregator.spec.PostAggregatorMetricSpec;
import org.bithon.server.metric.storage.DimensionCondition;
import org.bithon.server.metric.storage.GroupByQuery;
import org.bithon.server.metric.storage.IMetricReader;
import org.bithon.server.metric.storage.ListQuery;
import org.bithon.server.metric.storage.OrderBy;
import org.bithon.server.metric.storage.TimeseriesQuery;
import org.bithon.server.storage.jdbc.utils.SQLFilterBuilder;
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

    private final DSLContext dsl;
    private final ISqlExpressionFormatter sqlFormatter;

    public MetricJdbcReader(DSLContext dsl, ISqlExpressionFormatter sqlFormatter) {
        this.dsl = dsl;
        this.sqlFormatter = sqlFormatter;
    }

    @Override
    public List<Map<String, Object>> timeseries(TimeseriesQuery query) {
        String sql = new TimeSeriesSqlClauseBuilder(sqlFormatter,
                                                    query.getInterval().getStartTime(),
                                                    query.getInterval().getEndTime(),
                                                    query.getDataSource(),
                                                    query.getInterval().getStepLength()).filters(query.getFilters())
                                                                                        .metrics(query.getMetrics())
                                                                                        .groupBy(query.getGroupBys())
                                                                                        .build();
        return executeSql(sql);
    }

    @Override
    public List<Map<String, Object>> groupBy(GroupByQuery query) {
        String sqlTableName = "bithon_" + query.getDataSource().getName().replace("-", "_");

        MetricFieldsClauseBuilder metricFieldsBuilder = new MetricFieldsClauseBuilder(this.sqlFormatter,
                                                                                      sqlTableName,
                                                                                      "OUTER",
                                                                                      query.getDataSource(),
                                                                                      ImmutableMap.of("interval",
                                                                                                      query.getInterval().getTotalLength()));

        // put non-post aggregator metrics before the post
        String metricList = query.getMetrics()
                                 .stream()
                                 .map(m -> query.getDataSource().getMetricSpecByName(m))
                                 .filter(metricSpec -> !(metricSpec instanceof PostAggregatorMetricSpec))
                                 .map(metricSpec -> ", " + metricSpec.accept(metricFieldsBuilder))
                                 .collect(Collectors.joining());

        String postAggregatorMetrics = query.getMetrics()
                                            .stream()
                                            .map(m -> query.getDataSource().getMetricSpecByName(m))
                                            .filter(metricSpec -> (metricSpec instanceof PostAggregatorMetricSpec))
                                            .map(metricSpec -> ", " + metricSpec.accept(metricFieldsBuilder))
                                            .collect(Collectors.joining());

        String aggregatorList = query.getAggregators()
                                     .stream()
                                     .map(aggregator -> ", " + aggregator.accept(new QuerableAggregatorSqlVisitor()))
                                     .collect(Collectors.joining());

        String filter = SQLFilterBuilder.build(query.getFilters());

        String groupByFields = query.getGroupBys().stream().map(f -> "\"" + f + "\"").collect(Collectors.joining(","));

        String sql = StringUtils.format(
            "SELECT %s %s %s %s FROM \"%s\" OUTER WHERE %s %s \"timestamp\" >= %s AND \"timestamp\" < %s GROUP BY %s %s",
            groupByFields,
            metricList,
            postAggregatorMetrics,
            aggregatorList,
            sqlTableName,
            filter,
            StringUtils.hasText(filter) ? "AND" : "",
            sqlFormatter.formatTimestamp(query.getInterval().getStartTime()),
            sqlFormatter.formatTimestamp(query.getInterval().getEndTime()),
            groupByFields,
            getOrderBySQL(query.getOrderBy())
        );
        return executeSql(sql);
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

        String filter = SQLFilterBuilder.build(query.getFilters());
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

        String filter = SQLFilterBuilder.build(query.getFilters());
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
                                                           Collection<DimensionCondition> conditions,
                                                           String dimension) {
        String condition = conditions.stream()
                                     .map(d -> d.getMatcher().accept(new SQLFilterBuilder(d.getDimension())))
                                     .collect(Collectors.joining(" AND "));
        String sql = StringUtils.format(
            "SELECT DISTINCT(\"%s\") \"%s\" FROM \"%s\" WHERE %s AND \"timestamp\" >= %s AND \"timestamp\" < %s ORDER BY \"%s\"",
            dimension,
            dimension,
            "bithon_" + dataSourceSchema.getName().replace("-", "_"),
            condition,
            sqlFormatter.formatTimestamp(start),
            sqlFormatter.formatTimestamp(end),
            dimension
        );

        log.info("Executing {}", sql);
        List<Record> records = dsl.fetch(sql);
        return records.stream().map(record -> {
            Map<String, String> mapObject = new HashMap<>();
            for (Field<?> field : record.fields()) {
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

        /*
         * NOTE, H2 does not support timestamp comparison, we have to use ISO8601 format
         */
    }


    /**
     * build SQL clause which aggregates specified metric
     */
    public static class MetricFieldsClauseBuilder implements IMetricSpecVisitor<String> {

        private final String sqlTableName;
        private final String tableAlias;
        private final DataSourceSchema dataSource;
        private final boolean addAlias;
        private final Map<String, Object> variables;

        /**
         * used to keep which metrics current SQL are using
         */
        private final Set<String> metrics;
        private final ISqlExpressionFormatter sqlExpressionFormatter;

        public MetricFieldsClauseBuilder(ISqlExpressionFormatter sqlExpressionFormatter,
                                         String sqlTableName,
                                         String tableAlias,
                                         DataSourceSchema dataSource,
                                         Map<String, Object> variables) {
            this(sqlExpressionFormatter, sqlTableName, tableAlias, dataSource, variables, true);
        }

        public MetricFieldsClauseBuilder(ISqlExpressionFormatter sqlExpressionFormatter,
                                         String sqlTableName,
                                         String tableAlias,
                                         DataSourceSchema dataSource,
                                         Map<String, Object> variables,
                                         boolean addAlias) {
            this.sqlExpressionFormatter = sqlExpressionFormatter;
            this.sqlTableName = sqlTableName;
            this.tableAlias = tableAlias;
            this.dataSource = dataSource;
            this.variables = variables;
            this.addAlias = addAlias;
            this.metrics = new HashSet<>();
        }

        @Override
        public String visit(LongSumMetricSpec metricSpec) {
            this.metrics.add(metricSpec.getName());

            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.format("sum(\"%s\")", metricSpec.getName()));
            if (addAlias) {
                sb.append(StringUtils.format(" AS \"%s\"", metricSpec.getName()));
            }
            return sb.toString();
        }

        @Override
        public String visit(DoubleSumMetricSpec metricSpec) {
            this.metrics.add(metricSpec.getName());

            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.format("sum(\"%s\")", metricSpec.getName()));
            if (addAlias) {
                sb.append(StringUtils.format(" \"%s\"", metricSpec.getName()));
            }
            return sb.toString();
        }

        @Override
        public String visit(PostAggregatorMetricSpec metricSpec) {
            StringBuilder sb = new StringBuilder();
            metricSpec.visitExpression(new PostAggregatorExpressionVisitor() {
                @Override
                public void visitMetric(IMetricSpec metricSpec) {
                    if (!sqlExpressionFormatter.allowSameAggregatorExpression()
                        && metrics.contains(metricSpec.getName())) {
                        // this metricSpec has been in the SQL,
                        // don't need to construct the aggregation expression for this post aggregator
                        // because some DBMS does not support duplicated aggregation expressions for one metric
                        sb.append(metricSpec.getName());
                    } else {
                        sb.append(metricSpec.accept(new MetricFieldsClauseBuilder(sqlExpressionFormatter,
                                                                                  null,
                                                                                  null,
                                                                                  dataSource,
                                                                                  variables,
                                                                                  false)));
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
            this.metrics.add(metricSpec.getName());

            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.format("count(1)", metricSpec.getName()));
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
        public String visit(LongLastMetricSpec metricSpec) {
            return visitLast(metricSpec.getName());
        }

        @Override
        public String visit(DoubleLastMetricSpec metricSpec) {
            return visitLast(metricSpec.getName());
        }

        @Override
        public String visit(LongMinMetricSpec metricSpec) {
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.format("min(\"%s\")", metricSpec.getName()));
            if (addAlias) {
                sb.append(StringUtils.format(" \"%s\"", metricSpec.getName()));
            }
            return sb.toString();
        }

        @Override
        public String visit(LongMaxMetricSpec metricSpec) {
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.format("max(\"%s\")", metricSpec.getName()));
            if (addAlias) {
                sb.append(StringUtils.format(" \"%s\"", metricSpec.getName()));
            }
            return sb.toString();
        }

        private String visitLast(String metricName) {
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

    abstract static class MetricSpecVisitor implements IMetricSpecVisitor<Void> {
        @Override
        public Void visit(LongSumMetricSpec metricSpec) {
            visit(metricSpec, "sum");
            return null;
        }

        @Override
        public Void visit(DoubleSumMetricSpec metricSpec) {
            visit(metricSpec, "sum");
            return null;
        }

        /**
         * Since FIRST/LAST aggregators are not supported in many SQL databases,
         * A embedded query is created to simulate FIRST/LAST
         */
        @Override
        public Void visit(LongLastMetricSpec metricSpec) {
            visitLast(metricSpec.getName());
            return null;
        }

        @Override
        public Void visit(DoubleLastMetricSpec metricSpec) {
            visitLast(metricSpec.getName());
            return null;
        }

        @Override
        public Void visit(LongMinMetricSpec metricSpec) {
            visit(metricSpec, "min");
            return null;
        }

        @Override
        public Void visit(LongMaxMetricSpec metricSpec) {
            visit(metricSpec, "max");
            return null;
        }

        protected abstract void visit(IMetricSpec metricSpec, String aggregator);

        protected abstract void visitLast(String metricName);
    }

    static class TimeSeriesSqlClauseBuilder {
        private final List<String> postExpressions = new ArrayList<>(8);
        private final Set<String> rawExpressions = new HashSet<>();
        private final String tableName;
        private final DataSourceSchema schema;
        private final long interval;
        private final ISqlExpressionFormatter sqlFormatter;
        private final TimeSpan start;
        private final TimeSpan end;
        private String filters;
        private String groupBy = "";

        TimeSeriesSqlClauseBuilder(ISqlExpressionFormatter sqlFormatter,
                                   TimeSpan start,
                                   TimeSpan end,
                                   DataSourceSchema dataSourceSchema,
                                   long interval) {
            this.sqlFormatter = sqlFormatter;
            this.tableName = "bithon_" + dataSourceSchema.getName().replace("-", "_");
            this.start = start;
            this.end = end;
            this.schema = dataSourceSchema;
            this.interval = interval;
        }

        TimeSeriesSqlClauseBuilder metrics(Collection<String> metrics) {
            MetricClauseBuilder metricFieldsBuilder = new MetricClauseBuilder(this.sqlFormatter,
                                                                              ImmutableMap.of("interval",
                                                                                              interval,
                                                                                              "instanceCount",
                                                                                              "count(distinct \"instanceName\")"),
                                                                              true,
                                                                              postExpressions,
                                                                              rawExpressions);

            List<IMetricSpec> postMetricSpecList = new ArrayList<>();
            for (String metricName : metrics) {
                IMetricSpec metricSpec = schema.getMetricSpecByName(metricName);
                if (metricSpec == null) {
                    throw new RuntimeException(StringUtils.format("[%s] not defined", metricName));
                }

                if (metricSpec instanceof PostAggregatorMetricSpec) {
                    //
                    // post metrics will be processed at last
                    // so that there won't be duplicated expressions for one field
                    //
                    // This constraint is required by some DBMS
                    //
                    postMetricSpecList.add(metricSpec);
                } else {
                    metricSpec.accept(metricFieldsBuilder);
                }
            }
            for (IMetricSpec postMetricSpec : postMetricSpecList) {
                postMetricSpec.accept(metricFieldsBuilder);
            }

            if (!metricFieldsBuilder.hasLast) {
                this.rawExpressions.clear();
            }
            return this;
        }

        TimeSeriesSqlClauseBuilder filters(Collection<DimensionCondition> filters) {
            this.filters = filters.stream()
                                  .map(dimension -> dimension.getMatcher()
                                                             .accept(new SQLFilterBuilder(dimension.getDimension())))
                                  .collect(Collectors.joining(" AND "));
            return this;
        }

        public TimeSeriesSqlClauseBuilder groupBy(List<String> groupBy) {
            this.groupBy = groupBy.stream()
                                  .map(field -> "," + "\"" + schema.getDimensionSpecByName(field).getName() + "\"")
                                  .collect(Collectors.joining());
            return this;
        }

        String build() {
            String timestampExpression = sqlFormatter.timeFloor("timestamp", interval);
            if (rawExpressions.isEmpty()) {
                return StringUtils.format(
                    "SELECT %s AS \"_timestamp\", %s %s FROM \"%s\" WHERE %s AND \"timestamp\" >= %s AND \"timestamp\" < %s GROUP BY %s %s %s",
                    timestampExpression,
                    String.join(",", postExpressions),
                    this.groupBy,
                    tableName,
                    this.filters,
                    sqlFormatter.formatTimestamp(start),
                    sqlFormatter.formatTimestamp(end),
                    sqlFormatter.groupByUseRawExpression() ? timestampExpression : TIMESTAMP_QUERY_NAME,
                    this.groupBy,
                    // order by
                    sqlFormatter.orderByTimestamp(TIMESTAMP_QUERY_NAME)
                );
            } else {
                // quote must be used to brace the field name
                // otherwise in H2, the output field name will be turned into upper case
                return StringUtils.format(
                    "SELECT \"timestamp\" AS \"_timestamp\" %s ,%s FROM "
                    + "("
                    + "     SELECT %s, %s \"timestamp\" %s FROM \"%s\" WHERE %s AND \"timestamp\" >= %s AND \"timestamp\" < %s"
                    + ")GROUP BY \"%s\" %s %s",
                    this.groupBy,
                    String.join(",", postExpressions),
                    String.join(",", rawExpressions),
                    timestampExpression,
                    this.groupBy,
                    tableName,
                    this.filters,
                    sqlFormatter.formatTimestamp(start),
                    sqlFormatter.formatTimestamp(end),
                    // group by
                    sqlFormatter.groupByUseRawExpression() ? "timestamp" : TIMESTAMP_QUERY_NAME,
                    this.groupBy,
                    // order by
                    sqlFormatter.orderByTimestamp(TIMESTAMP_QUERY_NAME)
                );
            }
        }

        static class MetricClauseBuilder extends MetricSpecVisitor {
            private final ISqlExpressionFormatter sqlFormatter;
            private final List<String> postExpressions;
            private final Set<String> rawExpressions;
            private final boolean addAlias;
            private final Map<String, Object> variables;
            private final Set<String> metrics;
            private boolean hasLast;

            public MetricClauseBuilder(ISqlExpressionFormatter sqlFormatter,
                                       Map<String, Object> variables,
                                       boolean addAlias,
                                       List<String> postExpressions,
                                       Set<String> rawExpressions) {
                this.sqlFormatter = sqlFormatter;
                this.variables = variables;
                this.addAlias = addAlias;
                this.postExpressions = postExpressions;
                this.rawExpressions = rawExpressions;
                this.metrics = new HashSet<>();
            }

            @Override
            public Void visit(CountMetricSpec metricSpec) {
                this.metrics.add(metricSpec.getName());
                postExpressions.add(StringUtils.format("count(1)%s",
                                                       addAlias
                                                       ? StringUtils.format(" AS \"%s\"", metricSpec.getName())
                                                       : ""));
                rawExpressions.add(StringUtils.format(" \"%s\"", metricSpec.getName()));

                return null;
            }

            @Override
            public Void visit(PostAggregatorMetricSpec postMetricSpec) {
                StringBuilder sb = new StringBuilder();
                postMetricSpec.visitExpression(new PostAggregatorExpressionVisitor() {
                    @Override
                    public void visitMetric(IMetricSpec metricSpec) {
                        metricSpec.accept(new MetricSpecVisitor() {
                            @Override
                            protected void visit(IMetricSpec metricSpec, String aggregator) {
                                if (!sqlFormatter.allowSameAggregatorExpression()
                                    && metrics.contains(metricSpec.getName())) {
                                    sb.append(StringUtils.format("\"%s\"", metricSpec.getName()));
                                } else {
                                    sb.append(StringUtils.format("%s(\"%s\")", aggregator, metricSpec.getName()));
                                }
                                rawExpressions.add(StringUtils.format("\"%s\"", metricSpec.getName()));
                            }

                            @Override
                            protected void visitLast(String metricName) {
                                MetricClauseBuilder.this.visitLast(metricName);
                            }

                            @Override
                            public Void visit(CountMetricSpec metricSpec) {
                                rawExpressions.add(StringUtils.format("count(1)", metricSpec.getName()));
                                return null;
                            }

                            @Override
                            public Void visit(PostAggregatorMetricSpec metricSpec) {
                                throw new RuntimeException(StringUtils.format(
                                    "postAggregators [%s] can't be used on post aggregators [%s]",
                                    metricSpec.getName(),
                                    postMetricSpec.getName()));
                            }
                        });
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
                sb.append(StringUtils.format(" AS \"%s\"", postMetricSpec.getName()));

                postExpressions.add(sb.toString());

                return null;
            }

            @Override
            protected void visit(IMetricSpec metricSpec, String aggregator) {
                this.metrics.add(metricSpec.getName());
                postExpressions.add(StringUtils.format("%s(\"%s\")%s",
                                                       aggregator,
                                                       metricSpec.getName(),
                                                       addAlias
                                                       ? StringUtils.format(" AS \"%s\"", metricSpec.getName())
                                                       : ""));
                rawExpressions.add(StringUtils.format(" \"%s\"", metricSpec.getName()));
            }

            @Override
            protected void visitLast(String metricName) {
                this.hasLast = true;

                //postExpressions.add(StringUtils.format(" \"%s\"", metricName));
                postExpressions.add(StringUtils.format("sum(\"%s\")%s",
                                                       metricName,
                                                       addAlias ? StringUtils.format(" AS \"%s\"", metricName) : ""));

                int interval = ((Number) this.variables.get("interval")).intValue();
                rawExpressions.add(StringUtils.format(
                    "FIRST_VALUE(\"%s\") OVER (partition by %s ORDER BY \"timestamp\" DESC) \"%s\"",
                    metricName,
                    sqlFormatter.timeFloor("timestamp", interval),
                    metricName));
            }
        }
    }
}
