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
import org.bithon.server.common.matcher.AntPathMatcher;
import org.bithon.server.common.matcher.ContainsMatcher;
import org.bithon.server.common.matcher.EndwithMatcher;
import org.bithon.server.common.matcher.EqualMatcher;
import org.bithon.server.common.matcher.IContainsMatcher;
import org.bithon.server.common.matcher.IMatcherVisitor;
import org.bithon.server.common.matcher.RegexMatcher;
import org.bithon.server.common.matcher.StartwithMatcher;
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
import org.bithon.server.metric.storage.IMetricReader;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
class MetricJdbcReader implements IMetricReader {
    /**
     * Since we're writing some complex SQLs, we have to deal with different SQL syntax on different DBMS
     */
    interface ISQLProvider {
        default String timeFloor(String field, long interval) {
            return String.format("UNIX_TIMESTAMP(\"%s\")/ %d * %d", field, interval, interval);
        }

        default boolean groupByRawExpression() {
            return false;
        }

        default String formatTimestamp(TimeSpan timeSpan) {
            return "'" + timeSpan.toISO8601() + "'";
        }
    }

    static class DefaultSQLProvider implements ISQLProvider {
        public static ISQLProvider INSTANCE = new DefaultSQLProvider();
    }

    static class H2SQLProvider implements ISQLProvider {
        public static ISQLProvider INSTANCE = new H2SQLProvider();

        @Override
        public boolean groupByRawExpression() {
            return true;
        }

        /*
         * NOTE, H2 does not support timestamp comparison, we have to use ISO8601 format
         */
    }

    static class ClickHouseSQLProvider implements ISQLProvider {
        public static ISQLProvider INSTANCE = new ClickHouseSQLProvider();

        @Override
        public String timeFloor(String field, long interval) {
            return String.format("toUnixTimestamp(\"%s\")/ %d * %d", field, interval, interval);
        }

        /**
         * ClickHouse does not support ISO8601 very well, we treat it as timestamp
         */
        @Override
        public String formatTimestamp(TimeSpan timeSpan) {
            return String.format("fromUnixTimestamp(%d)", timeSpan.getMilliseconds());
        }
    }

    private final DSLContext dsl;
    private final ISQLProvider sqlProvider;

    public MetricJdbcReader(DSLContext dsl) {
        this.dsl = dsl;

        /*
         * we don't use the SQLDialect enum directly
         * because the enum 'CLICKHOUSE' is not supported by the current JOOQ(3.13~3.15) but by our modified version
         * using the string version so that it's compatible with these official libs
         */
        switch (dsl.dialect().name()) {
            case "H2":
                this.sqlProvider = H2SQLProvider.INSTANCE;
                break;
            case "CLICKHOUSE":
                this.sqlProvider = ClickHouseSQLProvider.INSTANCE;
                break;
            default:
                this.sqlProvider = DefaultSQLProvider.INSTANCE;
                break;
        }
    }

    // TODO: 具有多个纬度的聚合条件下，last/first应该按多个纬度分组求last/first，再聚合求和
    @Override
    public List<Map<String, Object>> timeseries(TimeSpan start,
                                                TimeSpan end,
                                                DataSourceSchema dataSourceSchema,
                                                Collection<DimensionCondition> filters,
                                                Collection<String> metrics,
                                                int interval) {
        String sql = new SQLClauseBuilder(sqlProvider, start, end, dataSourceSchema, interval).filters(filters)
                                                                                              .metrics(metrics)
                                                                                              .build();
        List<Map<String, Object>> queryResult = executeSql(sql);

        //
        // fill empty
        //
        List<Map<String, Object>> returns = new ArrayList<>();
        int j = 0;
        for (long slot = start.toSeconds() / interval * interval, endSlot = end.toSeconds() / interval * interval;
             slot < endSlot;
             slot += interval) {
            if (j < queryResult.size()) {
                long nextSlot = ((Number) queryResult.get(j).get("timestamp")).longValue();
                while (slot < nextSlot) {
                    Map<String, Object> empty = new HashMap<>(metrics.size());
                    empty.put("timestamp", slot * 1000);
                    metrics.forEach((metric) -> empty.put(metric, 0));
                    returns.add(empty);
                    slot += interval;
                }
                queryResult.get(j).put("timestamp", nextSlot * 1000);
                returns.add(queryResult.get(j++));
            } else {
                Map<String, Object> empty = new HashMap<>(metrics.size());
                empty.put("timestamp", slot * 1000);
                metrics.forEach((metric) -> empty.put(metric, 0));
                returns.add(empty);
            }
        }
        return returns;
    }

    @Override
    public Map<String, Object> getMetricValue(TimeSpan start,
                                              TimeSpan end,
                                              DataSourceSchema dataSourceSchema,
                                              Collection<DimensionCondition> filters,
                                              Collection<String> metrics) {
        String sql = new SQLClauseBuilder(sqlProvider, start,
                                          end,
                                          dataSourceSchema,
                                          (end.getMilliseconds() - start.getMilliseconds()) / 1000).filters(filters)
                                                                                                   .metrics(metrics)
                                                                                                   .build();
        List<Map<String, Object>> queryResult = executeSql(sql);

        List<Map<String, Object>> values = executeSql(sql);
        return CollectionUtils.isEmpty(values) ? Collections.emptyMap() : values.get(0);
    }

    @Override
    public List<Map<String, Object>> groupBy(TimeSpan start,
                                             TimeSpan end,
                                             DataSourceSchema dataSourceSchema,
                                             Collection<DimensionCondition> filter,
                                             Collection<String> metrics,
                                             Collection<String> groupBy) {
        String sqlTableName = "bithon_" + dataSourceSchema.getName().replace("-", "_");
        MetricFieldsClauseBuilder metricFieldsBuilder = new MetricFieldsClauseBuilder(sqlTableName,
                                                                                      "OUTER",
                                                                                      dataSourceSchema,
                                                                                      ImmutableMap.of("interval",
                                                                                                      (end.getMilliseconds()
                                                                                                       - start.getMilliseconds())
                                                                                                      / 1000));
        String metricList = metrics.stream()
                                   .map(m -> dataSourceSchema.getMetricSpecByName(m).accept(metricFieldsBuilder))
                                   .collect(Collectors.joining(", "));

        String condition = filter.stream()
                                 .map(dimension -> dimension.getMatcher()
                                                            .accept(new SQLFilterBuilder(dimension.getDimension())))
                                 .collect(Collectors.joining(" AND "));

        String groupByFields = groupBy.stream().map(f -> "\"" + f + "\"").collect(Collectors.joining(","));

        String sql = String.format(
            "SELECT %s, %s FROM \"%s\" %s WHERE %s AND \"timestamp\" >= '%s' AND \"timestamp\" <= '%s' GROUP BY %s",
            groupByFields,
            metricList,
            sqlTableName,
            "OUTER",
            condition,
            start.toISO8601(),
            end.toISO8601(),
            groupByFields
        );
        return executeSql(sql);
    }

    @Override
    public List<Map<String, Object>> executeSql(String sql) {
        log.info("Executing {}", sql);

        List<Record> records = dsl.fetch(sql);

        // PAY ATTENTION:
        //  although the explicit cast seems unnecessary, it must be kept so that compilation can pass
        //  this is might be a bug of JDK
        return (List<Map<String, Object>>) records.stream().map(record -> {
            Map<String, Object> mapObject = new HashMap<>();
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
        String sql = String.format(
            "SELECT DISTINCT(\"%s\") \"%s\" FROM \"%s\" WHERE %s AND \"timestamp\" >= %s AND \"timestamp\" <= %s ",
            dimension,
            dimension,
            "bithon_" + dataSourceSchema.getName().replace("-", "_"),
            condition,
            sqlProvider.formatTimestamp(start),
            sqlProvider.formatTimestamp(end)
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

    /**
     * build SQL where clause
     */
    static class SQLFilterBuilder implements IMatcherVisitor<String> {
        private final String name;

        SQLFilterBuilder(String name) {
            this.name = name;
        }

        @Override
        public String visit(EqualMatcher matcher) {
            StringBuilder sb = new StringBuilder(64);
            sb.append("\"");
            sb.append(name);
            sb.append("\"");
            sb.append("=");
            sb.append('\'');
            sb.append(matcher.getPattern());
            sb.append('\'');
            return sb.toString();
        }

        @Override
        public String visit(AntPathMatcher antPathMatcher) {
            return null;
        }

        @Override
        public String visit(ContainsMatcher containsMatcher) {
            return null;
        }

        @Override
        public String visit(EndwithMatcher endwithMatcher) {
            return null;
        }

        @Override
        public String visit(IContainsMatcher iContainsMatcher) {
            return null;
        }

        @Override
        public String visit(RegexMatcher regexMatcher) {
            return null;
        }

        @Override
        public String visit(StartwithMatcher startwithMatcher) {
            return null;
        }
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

        public MetricFieldsClauseBuilder(String sqlTableName,
                                         String tableAlias,
                                         DataSourceSchema dataSource,
                                         Map<String, Object> variables) {
            this(sqlTableName, tableAlias, dataSource, variables, true);
        }

        public MetricFieldsClauseBuilder(String sqlTableName,
                                         String tableAlias,
                                         DataSourceSchema dataSource,
                                         Map<String, Object> variables,
                                         boolean addAlias) {
            this.sqlTableName = sqlTableName;
            this.tableAlias = tableAlias;
            this.dataSource = dataSource;
            this.variables = variables;
            this.addAlias = addAlias;
        }

        @Override
        public String visit(LongSumMetricSpec metricSpec) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("sum(\"%s\")", metricSpec.getName()));
            if (addAlias) {
                sb.append(String.format(" \"%s\"", metricSpec.getName()));
            }
            return sb.toString();
        }

        @Override
        public String visit(DoubleSumMetricSpec metricSpec) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("sum(\"%s\")", metricSpec.getName()));
            if (addAlias) {
                sb.append(String.format(" \"%s\"", metricSpec.getName()));
            }
            return sb.toString();
        }

        @Override
        public String visit(PostAggregatorMetricSpec metricSpec) {
            StringBuilder sb = new StringBuilder();
            metricSpec.visitExpression(new PostAggregatorExpressionVisitor() {
                @Override
                public void visitMetric(IMetricSpec metricSpec) {
                    sb.append(metricSpec.accept(new MetricFieldsClauseBuilder(null,
                                                                              null,
                                                                              dataSource,
                                                                              variables,
                                                                              false)));
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
                        throw new RuntimeException(String.format("variable (%s) not provided in context", variable));
                    }
                    sb.append(variableValue);
                }
            });
            sb.append(String.format(" \"%s\"", metricSpec.getName()));
            return sb.toString();
        }

        @Override
        public String visit(CountMetricSpec metricSpec) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("sum(\"%s\")", metricSpec.getName()));
            if (addAlias) {
                sb.append(String.format(" \"%s\"", metricSpec.getName()));
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
            sb.append(String.format("min(\"%s\")", metricSpec.getName()));
            if (addAlias) {
                sb.append(String.format(" \"%s\"", metricSpec.getName()));
            }
            return sb.toString();
        }

        @Override
        public String visit(LongMaxMetricSpec metricSpec) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("max(\"%s\")", metricSpec.getName()));
            if (addAlias) {
                sb.append(String.format(" \"%s\"", metricSpec.getName()));
            }
            return sb.toString();
        }

        private String visitLast(String metricName) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(
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

        @Override
        public Void visit(CountMetricSpec metricSpec) {
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

    static class SQLClauseBuilder {
        private final List<String> postExpressions = new ArrayList<>(8);
        private final Set<String> rawExpressions = new HashSet<>();
        private final String tableName;
        private final DataSourceSchema schema;
        private final long interval;
        private final ISQLProvider sqlProvider;
        private String filters;
        private final TimeSpan start;
        private final TimeSpan end;

        static class MetricClauseBuilder extends MetricSpecVisitor {
            private final ISQLProvider sqlProvider;
            private final List<String> postExpressions;
            private final Set<String> rawExpressions;
            private final boolean addAlias;
            private final Map<String, Object> variables;
            private boolean hasLast;

            public MetricClauseBuilder(ISQLProvider sqlProvider,
                                       Map<String, Object> variables,
                                       boolean addAlias,
                                       List<String> postExpressions,
                                       Set<String> rawExpressions) {
                this.sqlProvider = sqlProvider;
                this.variables = variables;
                this.addAlias = addAlias;
                this.postExpressions = postExpressions;
                this.rawExpressions = rawExpressions;
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
                                sb.append(String.format("%s(\"%s\")", aggregator, metricSpec.getName()));

                                rawExpressions.add(String.format("\"%s\"", metricSpec.getName()));
                            }

                            @Override
                            protected void visitLast(String metricName) {
                                MetricClauseBuilder.this.visitLast(metricName);
                            }

                            @Override
                            public Void visit(PostAggregatorMetricSpec metricSpec) {
                                throw new RuntimeException(String.format(
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
                            throw new RuntimeException(String.format("variable (%s) not provided in context",
                                                                     variable));
                        }
                        sb.append(variableValue);
                    }
                });
                sb.append(String.format(" \"%s\"", postMetricSpec.getName()));

                postExpressions.add(sb.toString());

                return null;
            }

            @Override
            protected void visit(IMetricSpec metricSpec, String aggregator) {
                postExpressions.add(String.format("%s(\"%s\")%s",
                                                  aggregator,
                                                  metricSpec.getName(),
                                                  addAlias ? String.format(" \"%s\"", metricSpec.getName()) : ""));

                rawExpressions.add(String.format(" \"%s\"", metricSpec.getName()));
            }

            @Override
            protected void visitLast(String metricName) {
                this.hasLast = true;

                postExpressions.add(String.format(" \"%s\"", metricName));

                int interval = ((Number) this.variables.get("interval")).intValue();
                rawExpressions.add(String.format("FIRST_VALUE(\"%s\") OVER (partition by %s ORDER BY \"timestamp\" DESC) \"%s\"",
                                                 metricName,
                                                 sqlProvider.timeFloor("timestamp", interval),
                                                 metricName));
            }
        }

        SQLClauseBuilder(ISQLProvider sqlProvider,
                         TimeSpan start,
                         TimeSpan end,
                         DataSourceSchema dataSourceSchema,
                         long interval) {
            this.sqlProvider = sqlProvider;
            this.tableName = "bithon_" + dataSourceSchema.getName().replace("-", "_");
            this.start = start;
            this.end = end;
            this.schema = dataSourceSchema;
            this.interval = interval;
        }

        SQLClauseBuilder metrics(Collection<String> metrics) {
            MetricClauseBuilder metricFieldsBuilder = new MetricClauseBuilder(this.sqlProvider,
                                                                              ImmutableMap.of("interval",
                                                                                              interval,
                                                                                              //TODO: use the quote based on the SQL dialect
                                                                                              "instanceCount",
                                                                                              "count(distinct \"instanceName\")"),
                                                                              true,
                                                                              postExpressions,
                                                                              rawExpressions);
            for (String metric : metrics) {
                schema.getMetricSpecByName(metric).accept(metricFieldsBuilder);
            }
            if (!metricFieldsBuilder.hasLast) {
                this.rawExpressions.clear();
            }
            return this;
        }

        SQLClauseBuilder filters(Collection<DimensionCondition> filters) {
            this.filters = filters.stream()
                                  .map(dimension -> dimension.getMatcher().accept(new SQLFilterBuilder(dimension.getDimension())))
                                  .collect(Collectors.joining(" AND "));
            return this;
        }

        String build() {
            String groupByExpression = sqlProvider.timeFloor("timestamp", interval);
            if (rawExpressions.isEmpty()) {
                return String.format(
                    "SELECT %s \"timestamp\", %s FROM \"%s\" %s WHERE %s AND \"timestamp\" >= %s AND \"timestamp\" <= %s GROUP BY %s",
                    groupByExpression,
                    String.join(",", postExpressions),
                    tableName,
                    "OUTER",
                    this.filters,
                    sqlProvider.formatTimestamp(start),
                    sqlProvider.formatTimestamp(end),
                    sqlProvider.groupByRawExpression() ? groupByExpression : "timestamp"
                );
            } else {
                return String.format(
                    "SELECT \"timestamp\", %s FROM "
                    + "("
                    + "     SELECT %s, %s \"timestamp\" FROM \"%s\" WHERE %s AND \"timestamp\" >= %s AND \"timestamp\" <= %s"
                    + ")GROUP BY \"timestamp\"",
                    String.join(",", postExpressions),
                    String.join(",", rawExpressions),
                    groupByExpression,
                    tableName,
                    this.filters,
                    sqlProvider.formatTimestamp(start),
                    sqlProvider.formatTimestamp(end)
                );
            }
        }
    }
}
