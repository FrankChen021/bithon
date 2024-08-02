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

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.common.expression.ExpressionASTBuilder;
import org.bithon.server.storage.datasource.DefaultSchema;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.ExpressionColumn;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.storage.datasource.column.aggregatable.last.AggregateLongLastColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.datasource.query.Order;
import org.bithon.server.storage.datasource.query.OrderBy;
import org.bithon.server.storage.datasource.query.ast.Alias;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.QueryExpression;
import org.bithon.server.storage.datasource.query.ast.QueryStageFunctions;
import org.bithon.server.storage.datasource.query.ast.Selector;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.metrics.Interval;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * @author frank.chen021@outlook.com
 * @date 26/7/24 10:44 am
 */
public class QueryExpressionBuilderTest {

    private final ISchema schema = new DefaultSchema("bithon-jvm-metrics",
                                                     "bithon-jvm-metrics",
                                                     new TimestampSpec("timestamp"),
                                                     Arrays.asList(new StringColumn("appName", "appName"), new StringColumn("instance", "instance")),
                                                     Arrays.asList(new AggregateLongSumColumn("responseTime", "responseTime"),
                                                                   new AggregateLongSumColumn("totalCount", "totalCount"),
                                                                   new AggregateLongSumColumn("count4xx", "count4xx"),
                                                                   new AggregateLongSumColumn("count5xx", "count5xx"),
                                                                   new AggregateLongLastColumn("activeThreads", "activeThreads"),
                                                                   new AggregateLongLastColumn("totalThreads", "totalThreads"),
                                                                   new ExpressionColumn("avgResponseTime", null, "sum(responseTime) / sum(totalCount)", "double")
                                                     ),
                                                     null,
                                                     new IDataStoreSpec() {
                                                         @Override
                                                         public String getStore() {
                                                             return "bithon_jvm_metrics";
                                                         }

                                                         @Override
                                                         public void setSchema(ISchema schema) {

                                                         }

                                                         @Override
                                                         public boolean isInternal() {
                                                             return false;
                                                         }

                                                         @Override
                                                         public IDataSourceReader createReader() {
                                                             return null;
                                                         }
                                                     },
                                                     null,
                                                     null);

    final ISqlDialect h2Dialect = new ISqlDialect() {

        @Override
        public String formatTimestamp(TimeSpan timeSpan) {
            return "'" + timeSpan.toISO8601(TimeZone.getTimeZone("GMT+8:00")) + "'";
        }

        @Override
        public boolean useWindowFunctionAsAggregator(String aggregator) {
            return "first".equals(aggregator) || "last".equals(aggregator);
        }

        @Override
        public String quoteIdentifier(String identifier) {
            return "\"" + identifier + "\"";
        }

        @Override
        public String timeFloorExpression(IExpression timestampExpression, long intervalSeconds) {
            return StringUtils.format("UNIX_TIMESTAMP(%s)/ %d * %d", timestampExpression.serializeToText(), intervalSeconds, intervalSeconds);
        }

        @Override
        public boolean groupByUseRawExpression() {
            return false;
        }

        @Override
        public boolean allowSameAggregatorExpression() {
            return false;
        }

        @Override
        public String stringAggregator(String field) {
            return "";
        }

        @Override
        public String firstAggregator(String field, long window) {
            return StringUtils.format(
                "FIRST_VALUE(\"%s\") OVER (partition by %s ORDER BY \"timestamp\")",
                field,
                this.timeFloorExpression(new IdentifierExpression("timestamp"), window));
        }

        @Override
        public String lastAggregator(String field, long window) {
            // NOTE: use FIRST_VALUE instead of LAST_VALUE because the latter one returns the wrong result
            return StringUtils.format(
                "FIRST_VALUE(\"%s\") OVER (partition by %s ORDER BY \"timestamp\" DESC)",
                field,
                this.timeFloorExpression(new IdentifierExpression("timestamp"), window));
        }

        @Override
        public String formatDateTime(LiteralExpression.TimestampLiteral expression) {
            return "";
        }

        @Override
        public char getEscapeCharacter4SingleQuote() {
            return 0;
        }
    };

    final ISqlDialect clickHouseDialect = new ISqlDialect() {
        @Override
        public String formatTimestamp(TimeSpan timeSpan) {
            return "'" + timeSpan.toISO8601(TimeZone.getTimeZone("GMT+8:00")) + "'";
        }

        @Override
        public String quoteIdentifier(String identifier) {
            return "\"" + identifier + "\"";
        }

        @Override
        public String timeFloorExpression(IExpression timestampExpression, long intervalSeconds) {
            return StringUtils.format("UNIX_TIMESTAMP(%s)/ %d * %d", timestampExpression.serializeToText(), intervalSeconds, intervalSeconds);
        }

        @Override
        public boolean groupByUseRawExpression() {
            return false;
        }

        @Override
        public boolean allowSameAggregatorExpression() {
            return false;
        }

        @Override
        public String stringAggregator(String field) {
            return "";
        }

        @Override
        public String firstAggregator(String field, long window) {
            return StringUtils.format("argMin(%s, %s)", quoteIdentifier(field), quoteIdentifier("timestamp"));
        }

        @Override
        public String lastAggregator(String field, long window) {
            return StringUtils.format("argMax(%s, %s)", quoteIdentifier(field), quoteIdentifier("timestamp"));
        }

        @Override
        public String formatDateTime(LiteralExpression.TimestampLiteral expression) {
            return "";
        }

        @Override
        public char getEscapeCharacter4SingleQuote() {
            return 0;
        }
    };

    @BeforeClass
    public static void setUp() {
        new QueryStageFunctions().afterPropertiesSet();
    }

    @Test
    public void testSimpleAggregation_GroupBy() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("sum(totalCount)"), new Alias("t"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   sum("totalCount") AS "t"
                            FROM "bithon_jvm_metrics"
                            WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                            GROUP BY "appName"
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testExpressionInAggregation_GroupBy() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("sum(totalCount*2)"), new Alias("t"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   sum("totalCount" * 2) AS "t"
                            FROM "bithon_jvm_metrics"
                            WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                            GROUP BY "appName"
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testSimpleAggregation_TimeSeries() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("sum(totalCount)"), new Alias("totalCount"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      new IdentifierExpression("timestamp")
                                                                ))
                                                                .groupBy(List.of("appName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                   "appName",
                                   sum("totalCount") AS "totalCount"
                            FROM "bithon_jvm_metrics"
                            WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                            GROUP BY "appName", "_timestamp"
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testPostAggregation_GroupBy() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("sum(responseTime*2)/sum(totalCount)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   "_var0" / "totalCount" AS "avg"
                            FROM
                            (
                              SELECT "appName",
                                     "instanceName",
                                     sum("responseTime" * 2) AS "_var0",
                                     sum("totalCount") AS "totalCount"
                              FROM "bithon_jvm_metrics"
                              WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                              GROUP BY "appName", "instanceName"
                            ) AS "tbl1"
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testPostAggregation_GroupBy_NestedFunction() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("round(round(sum(responseTime)/sum(totalCount),2), 2)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   round(round("responseTime" / "totalCount", 2), 2) AS "avg"
                            FROM
                            (
                              SELECT "appName",
                                     "instanceName",
                                     sum("responseTime") AS "responseTime",
                                     sum("totalCount") AS "totalCount"
                              FROM "bithon_jvm_metrics"
                              WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                              GROUP BY "appName", "instanceName"
                            ) AS "tbl1"
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testPostAggregation_TimeSeries() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("sum(responseTime)/sum(totalCount)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      new IdentifierExpression("timestamp")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "_timestamp",
                                   "appName",
                                   "instanceName",
                                   "responseTime" / "totalCount" AS "avg"
                            FROM
                            (
                              SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                     "appName",
                                     "instanceName",
                                     sum("responseTime") AS "responseTime",
                                     sum("totalCount") AS "totalCount"
                              FROM "bithon_jvm_metrics"
                              WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                              GROUP BY "appName", "instanceName", "_timestamp"
                            ) AS "tbl1"
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testPostFunctionExpression_GroupBy() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("round(sum(responseTime)/sum(totalCount), 2)"),
                                                                                                               new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   round("responseTime" / "totalCount", 2) AS "avg"
                            FROM
                            (
                              SELECT "appName",
                                     "instanceName",
                                     sum("responseTime") AS "responseTime",
                                     sum("totalCount") AS "totalCount"
                              FROM "bithon_jvm_metrics"
                              WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                              GROUP BY "appName", "instanceName"
                            ) AS "tbl1"
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testDuplicateAggregations() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(List.of(new Selector(new Expression("sum(count4xx) + sum(count5xx)"), new Alias("errorCount")),
                                                                                new Selector(new Expression("round((sum(count4xx) + sum(count5xx))*100.0/sum(totalCount), 2)"), new Alias("errorRate"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   "count4xx" + "count5xx" AS "errorCount",
                                   round((("count4xx" + "count5xx") * 100.0) / "totalCount", 2) AS "errorRate"
                            FROM
                            (
                              SELECT "appName",
                                     "instanceName",
                                     sum("count4xx") AS "count4xx",
                                     sum("count5xx") AS "count5xx",
                                     sum("totalCount") AS "totalCount"
                              FROM "bithon_jvm_metrics"
                              WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                              GROUP BY "appName", "instanceName"
                            ) AS "tbl1"
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testWindowFunction_GroupBy() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("first(activeThreads)"), new Alias("a"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .filter(new ComparisonExpression.GT(new IdentifierExpression("a"), new LiteralExpression.LongLiteral(5)))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   "a"
                            FROM
                            (
                              SELECT "appName",
                                     "instanceName",
                                     FIRST_VALUE("activeThreads") OVER (partition by UNIX_TIMESTAMP("timestamp")/ 600 * 600 ORDER BY "timestamp") AS "a"
                              FROM "bithon_jvm_metrics"
                              WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                            ) AS "tbl1"
                            GROUP BY "appName", "instanceName", "a"
                            HAVING "a" > 5
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testWindowFunction_GroupBy_NoUseWindowAggregator() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(clickHouseDialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("first(activeThreads)"), new Alias("a"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(clickHouseDialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   argMin("activeThreads", "timestamp") AS "a"
                            FROM "bithon_jvm_metrics"
                            WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                            GROUP BY "appName", "instanceName"
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testWindowFunction_TimeSeries() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("first(activeThreads)"), new Alias("activeThreads"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      new IdentifierExpression("timestamp")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "_timestamp",
                                   "appName",
                                   "instanceName",
                                   "activeThreads"
                            FROM
                            (
                              SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                     "appName",
                                     "instanceName",
                                     FIRST_VALUE("activeThreads") OVER (partition by UNIX_TIMESTAMP("timestamp")/ 600 * 600 ORDER BY "timestamp") AS "activeThreads"
                              FROM "bithon_jvm_metrics"
                              WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                            ) AS "tbl1"
                            GROUP BY "appName", "instanceName", "activeThreads", "_timestamp"
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testWindowFunction_WithAggregator() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("first(activeThreads)/sum(totalThreads)"), new Alias("ratio"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .orderBy(OrderBy.builder().name("timestamp").order(Order.asc).build())
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   "activeThreads" / "totalThreads" AS "ratio"
                            FROM
                            (
                              SELECT "appName",
                                     "instanceName",
                                     "activeThreads",
                                     sum("totalThreads") AS "totalThreads"
                              FROM
                              (
                                SELECT "appName",
                                       "instanceName",
                                       FIRST_VALUE("activeThreads") OVER (partition by UNIX_TIMESTAMP("timestamp")/ 600 * 600 ORDER BY "timestamp") AS "activeThreads",
                                       "totalThreads"
                                FROM "bithon_jvm_metrics"
                                WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                              ) AS "tbl1"
                              GROUP BY "appName", "instanceName", "activeThreads"
                            ) AS "tbl2"
                            ORDER BY "timestamp" asc
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testWindowFunction_NoUseWindowAggregator_WithAggregator() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(clickHouseDialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("first(activeThreads)/sum(totalThreads)"), new Alias("ratio"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .orderBy(OrderBy.builder().name("timestamp").order(Order.asc).build())
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(clickHouseDialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   "activeThreads" / "totalThreads" AS "ratio"
                            FROM
                            (
                              SELECT "appName",
                                     "instanceName",
                                     argMin("activeThreads", "timestamp") AS "activeThreads",
                                     sum("totalThreads") AS "totalThreads"
                              FROM "bithon_jvm_metrics"
                              WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                              GROUP BY "appName", "instanceName"
                            ) AS "tbl1"
                            ORDER BY "timestamp" asc
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testWindowFunctionAfterAggregator() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("sum(totalThreads) - first(activeThreads)"), new Alias("daemon"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .orderBy(OrderBy.builder().name("timestamp").order(Order.asc).build())
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   "totalThreads" - "activeThreads" AS "daemon"
                            FROM
                            (
                              SELECT "appName",
                                     "instanceName",
                                     sum("totalThreads") AS "totalThreads",
                                     "activeThreads"
                              FROM
                              (
                                SELECT "appName",
                                       "instanceName",
                                       FIRST_VALUE("activeThreads") OVER (partition by UNIX_TIMESTAMP("timestamp")/ 600 * 600 ORDER BY "timestamp") AS "activeThreads",
                                       "totalThreads"
                                FROM "bithon_jvm_metrics"
                                WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                              ) AS "tbl1"
                              GROUP BY "appName", "instanceName", "activeThreads"
                            ) AS "tbl2"
                            ORDER BY "timestamp" asc
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testAggregationWithMacroExpression() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("sum(totalCount)/{interval}"), new Alias("qps"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      new IdentifierExpression("timestamp")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .orderBy(OrderBy.builder().name("appName").order(Order.asc).build())
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "_timestamp",
                                   "appName",
                                   "instanceName",
                                   "totalCount" / 10 AS "qps"
                            FROM
                            (
                              SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                     "appName",
                                     "instanceName",
                                     sum("totalCount") AS "totalCount"
                              FROM "bithon_jvm_metrics"
                              WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                              GROUP BY "appName", "instanceName", "_timestamp"
                            ) AS "tbl1"
                            ORDER BY "appName" asc
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testCardinalityAggregation() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("cardinality(instance)"), new Alias("instanceCount"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .orderBy(OrderBy.builder().name("appName").order(Order.asc).build())
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   count(distinct "instance") AS "instanceCount"
                            FROM "bithon_jvm_metrics"
                            WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                            GROUP BY "appName"
                            ORDER BY "appName" asc
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testCountAggregation() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("count(1)"), new Alias("cnt"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .orderBy(OrderBy.builder().name("appName").order(Order.asc).build())
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   count(1) AS "cnt"
                            FROM "bithon_jvm_metrics"
                            WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                            GROUP BY "appName"
                            ORDER BY "appName" asc
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testHumanReadableLiteral() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("count(1)"), new Alias("cnt"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .filter(new LogicalExpression.AND(new ComparisonExpression.GT(new IdentifierExpression("totalCount"), new LiteralExpression.ReadableNumberLiteral(HumanReadableNumber.of("1MiB"))),
                                                                                                  new ComparisonExpression.LT(new IdentifierExpression("totalCount"), new LiteralExpression.ReadableDurationLiteral(HumanReadableDuration.parse("1h"))),
                                                                                                  new ComparisonExpression.LT(new IdentifierExpression("totalCount"), new LiteralExpression.ReadablePercentageLiteral(HumanReadablePercentage.parse("50%")))
                                                                ))
                                                                .groupBy(List.of("appName"))
                                                                .orderBy(OrderBy.builder().name("appName").order(Order.asc).build())
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   count(1) AS "cnt"
                            FROM "bithon_jvm_metrics"
                            WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00' AND ("bithon_jvm_metrics"."totalCount" > 1048576 AND "bithon_jvm_metrics"."totalCount" < 3600 AND "bithon_jvm_metrics"."totalCount" < 0.5)
                            GROUP BY "appName"
                            ORDER BY "appName" asc
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    /**
     * The filter is based on an expression
     */
    @Test
    public void testPostFilter_UseExpressionVariable() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("sum(responseTime*2)/sum(totalCount)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .filter(
                                                                    new LogicalExpression.AND(
                                                                        new ComparisonExpression.EQ(new IdentifierExpression("appName"), new LiteralExpression.StringLiteral("bithon")),
                                                                        new ComparisonExpression.GT(new IdentifierExpression("avg"), new LiteralExpression.DoubleLiteral(0.2))
                                                                    )
                                                                )
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   "_var0" / "totalCount" AS "avg"
                            FROM
                            (
                              SELECT "appName",
                                     "instanceName",
                                     sum("responseTime" * 2) AS "_var0",
                                     sum("totalCount") AS "totalCount"
                              FROM "bithon_jvm_metrics"
                              WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00' AND "bithon_jvm_metrics"."appName" = 'bithon'
                              GROUP BY "appName", "instanceName"
                            ) AS "tbl1"
                            WHERE "avg" > 0.2
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    /**
     * The filter is based on an expression
     */
    @Test
    public void testPostFilter_UseAggregationAliasInFilter() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("sum(totalCount)"), new Alias("cnt"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .filter(new ComparisonExpression.GT(new IdentifierExpression("cnt"), new LiteralExpression.LongLiteral(1000)))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   sum("totalCount") AS "cnt"
                            FROM "bithon_jvm_metrics"
                            WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                            GROUP BY "appName", "instanceName"
                            HAVING "cnt" > 1000
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    /**
     * The filter is based on an expression
     */
    @Test
    public void testPostFilter_UseAggregationFilter() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("sum(totalCount)"), new Alias("totalCount"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .filter(new ComparisonExpression.GT(new IdentifierExpression("totalCount"), new LiteralExpression.LongLiteral(1000)))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   sum("totalCount") AS "totalCount"
                            FROM "bithon_jvm_metrics"
                            WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                            GROUP BY "appName", "instanceName"
                            HAVING "totalCount" > 1000
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testPostFilter_FilterNotInTheSelectList() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("sum(totalCount)"), new Alias("totalCount"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .filter(ExpressionASTBuilder.builder()
                                                                                            .schema(schema)
                                                                                            .functions(Functions.getInstance())
                                                                                            .build("avgResponseTime > 5"))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   "responseTime" / "totalCount" AS "avgResponseTime"
                            FROM
                            (
                                SELECT "appName",
                                       "instanceName",
                                       sum("totalCount") AS "totalCount",
                                       sum("responseTime") AS "responseTime"
                                FROM "bithon_jvm_metrics"
                                WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                                GROUP BY "appName", "instanceName"
                            )
                            WHERE "avgResponseTime" > 5
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testPostFilter_AggregationInFilter() {
        QueryExpression queryExpression = QueryExpressionBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression("sum(totalCount)"), new Alias("totalCount"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .filter(ExpressionASTBuilder.builder()
                                                                                            .schema(schema)
                                                                                            .functions(Functions.getInstance())
                                                                                            .build("sum(responseTime) / sum(totalCount) > 5"))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .dataSource(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                                   "instanceName",
                                   "totalCount" AS "totalCount",
                                   "responseTime" / "totalCount" AS "_var0"
                            FROM
                            (
                                SELECT "appName",
                                       "instanceName",
                                       sum("totalCount") AS "totalCount",
                                       sum("responseTime") AS "responseTime"
                                FROM "bithon_jvm_metrics"
                                WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                                GROUP BY "appName", "instanceName"
                            )
                            WHERE "_var0" > 5
                            """.trim(),
                            sqlGenerator.getSQL());
    }
}
