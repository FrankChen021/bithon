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

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.DefaultSchema;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.storage.datasource.column.aggregatable.last.AggregateLongLastColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.datasource.query.Order;
import org.bithon.server.storage.datasource.query.OrderBy;
import org.bithon.server.storage.datasource.query.ast.ColumnAlias;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.QueryExpression;
import org.bithon.server.storage.datasource.query.ast.SelectColumn;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.metrics.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 26/7/24 10:44 am
 */
public class SelectExpressionBuilderTest {

    private final ISchema schema = new DefaultSchema("bithon-jvm-metrics",
                                                     "bithon-jvm-metrics",
                                                     new TimestampSpec("timestamp"),
                                                     Arrays.asList(new StringColumn("appName", "appName"), new StringColumn("instance", "instance")),
                                                     Arrays.asList(new AggregateLongSumColumn("responseTime", "responseTime"),
                                                                   new AggregateLongSumColumn("totalCount", "totalCount"),
                                                                   new AggregateLongSumColumn("count4xx", "count4xx"),
                                                                   new AggregateLongSumColumn("count5xx", "count5xx"),
                                                                   new AggregateLongLastColumn("activeThreads", "activeThreads"),
                                                                   new AggregateLongLastColumn("totalThreads", "totalThreads")
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

    private final ISqlDialect dialect = new ISqlDialect() {
        @Override
        public String quoteIdentifier(String identifier) {
            return identifier;
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
            return "";
        }

        @Override
        public String lastAggregator(String field, long window) {
            return "";
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

    final ISqlDialect h2Dialect = new ISqlDialect() {
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

    @Test
    public void testSimpleAggregation_GroupBy() {
        QueryExpression queryExpression = SelectExpressionBuilder.builder()
                                                                 .sqlDialect(dialect)
                                                                 .fields(Collections.singletonList(new SelectColumn(new Expression("sum(totalCount)"), new ColumnAlias("totalCount"))))
                                                                 .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                 .groupBys(List.of("appName"))
                                                                 .dataSource(schema)
                                                                 .buildPipeline();

        SqlGenerator sqlGenerator = new SqlGenerator(dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT appName,
                            sum(totalCount) AS totalCount
                            FROM bithon_jvm_metrics
                            WHERE timestamp >= '2024-07-26T21:22:00.000+08:00' AND timestamp < '2024-07-26T21:32:00.000+08:00'
                            GROUP BY appName
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testSimpleAggregation_TimeSeries() {
        QueryExpression queryExpression = SelectExpressionBuilder.builder()
                                                                 .sqlDialect(dialect)
                                                                 .fields(Collections.singletonList(new SelectColumn(new Expression("sum(totalCount)"), new ColumnAlias("totalCount"))))
                                                                 .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                       TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                       Duration.ofSeconds(10),
                                                                                       new IdentifierExpression("timestamp")
                                                                                       ))
                                                                 .groupBys(List.of("appName"))
                                                                 .dataSource(schema)
                                                                 .buildPipeline();

        SqlGenerator sqlGenerator = new SqlGenerator(dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS _timestamp,
                            appName,
                            sum(totalCount) AS totalCount
                            FROM bithon_jvm_metrics
                            WHERE timestamp >= '2024-07-26T21:22:00.000+08:00' AND timestamp < '2024-07-26T21:32:00.000+08:00'
                            GROUP BY appName, _timestamp
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testPostCalculationExpression_GroupBy() {
        QueryExpression queryExpression = SelectExpressionBuilder.builder()
                                                                 .sqlDialect(dialect)
                                                                 .fields(Collections.singletonList(new SelectColumn(new Expression("sum(responseTime)/sum(totalCount)"), new ColumnAlias("avg"))))
                                                                 .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                 .groupBys(List.of("appName", "instanceName"))
                                                                 .dataSource(schema)
                                                                 .buildPipeline();

        SqlGenerator sqlGenerator = new SqlGenerator(dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT appName,
                            instanceName,
                            responseTime / totalCount AS avg
                            FROM
                            (
                              SELECT appName,
                              instanceName,
                              sum(responseTime) AS responseTime,
                              sum(totalCount) AS totalCount
                              FROM bithon_jvm_metrics
                              WHERE timestamp >= '2024-07-26T21:22:00.000+08:00' AND timestamp < '2024-07-26T21:32:00.000+08:00'
                              GROUP BY appName, instanceName
                            ) AS tbl1
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testPostCalculationExpression_TimeSeries() {
        QueryExpression queryExpression = SelectExpressionBuilder.builder()
                                                                 .sqlDialect(dialect)
                                                                 .fields(Collections.singletonList(new SelectColumn(new Expression("sum(responseTime)/sum(totalCount)"), new ColumnAlias("avg"))))
                                                                 .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                       TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                       Duration.ofSeconds(10),
                                                                                       new IdentifierExpression("timestamp")))
                                                                 .groupBys(List.of("appName", "instanceName"))
                                                                 .dataSource(schema)
                                                                 .buildPipeline();

        SqlGenerator sqlGenerator = new SqlGenerator(dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT _timestamp,
                            appName,
                            instanceName,
                            responseTime / totalCount AS avg
                            FROM
                            (
                              SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS _timestamp,
                              appName,
                              instanceName,
                              sum(responseTime) AS responseTime,
                              sum(totalCount) AS totalCount
                              FROM bithon_jvm_metrics
                              WHERE timestamp >= '2024-07-26T21:22:00.000+08:00' AND timestamp < '2024-07-26T21:32:00.000+08:00'
                              GROUP BY appName, instanceName, _timestamp
                            ) AS tbl1
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testPostFunctionExpression_GroupBy() {
        QueryExpression queryExpression = SelectExpressionBuilder.builder()
                                                                 .sqlDialect(dialect)
                                                                 .fields(Collections.singletonList(new SelectColumn(new Expression("round(sum(responseTime)/sum(totalCount), 2)"),
                                                                                                                    new ColumnAlias("avg"))))
                                                                 .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                       TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                 .groupBys(List.of("appName", "instanceName"))
                                                                 .dataSource(schema)
                                                                 .buildPipeline();

        SqlGenerator sqlGenerator = new SqlGenerator(dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT appName,
                            instanceName,
                            round(responseTime / totalCount, 2) AS avg
                            FROM
                            (
                              SELECT appName,
                              instanceName,
                              sum(responseTime) AS responseTime,
                              sum(totalCount) AS totalCount
                              FROM bithon_jvm_metrics
                              WHERE timestamp >= '2024-07-26T21:22:00.000+08:00' AND timestamp < '2024-07-26T21:32:00.000+08:00'
                              GROUP BY appName, instanceName
                            ) AS tbl1
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testDuplicateAggregations() {
        QueryExpression queryExpression = SelectExpressionBuilder.builder()
                                                                 .sqlDialect(h2Dialect)
                                                                 .fields(List.of(new SelectColumn(new Expression("sum(count4xx) + sum(count5xx)"), new ColumnAlias("errorCount")),
                                                                                 new SelectColumn(new Expression("round((sum(count4xx) + sum(count5xx))*100.0/sum(totalCount), 2)"), new ColumnAlias("errorRate"))))
                                                                 .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                 .groupBys(List.of("appName", "instanceName"))
                                                                 .dataSource(schema)
                                                                 .buildPipeline();

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
        QueryExpression queryExpression = SelectExpressionBuilder.builder()
                                                                 .sqlDialect(h2Dialect)
                                                                 .fields(Collections.singletonList(new SelectColumn(new Expression("first(activeThreads)"), new ColumnAlias("activeThreads"))))
                                                                 .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                 .groupBys(List.of("appName", "instanceName"))
                                                                 .dataSource(schema)
                                                                 .buildPipeline();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
                            "instanceName",
                            "activeThreads"
                            FROM
                            (
                              SELECT "appName",
                              "instanceName",
                              FIRST_VALUE("activeThreads") OVER (partition by UNIX_TIMESTAMP("timestamp")/ 600 * 600 ORDER BY "timestamp") AS "activeThreads"
                              FROM "bithon_jvm_metrics"
                              WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                            ) AS "tbl1"
                            GROUP BY "appName", "instanceName", "activeThreads"
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testWindowFunction_TimeSeries() {
        QueryExpression queryExpression = SelectExpressionBuilder.builder()
                                                                 .sqlDialect(h2Dialect)
                                                                 .fields(Collections.singletonList(new SelectColumn(new Expression("first(activeThreads)"), new ColumnAlias("activeThreads"))))
                                                                 .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                       TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                       Duration.ofSeconds(10),
                                                                                       new IdentifierExpression("timestamp")))
                                                                 .groupBys(List.of("appName", "instanceName"))
                                                                 .dataSource(schema)
                                                                 .buildPipeline();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "appName",
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
        QueryExpression queryExpression = SelectExpressionBuilder.builder()
                                                                 .sqlDialect(h2Dialect)
                                                                 .fields(Collections.singletonList(new SelectColumn(new Expression("first(activeThreads)/sum(totalThreads)"), new ColumnAlias("ratio"))))
                                                                 .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                 .groupBys(List.of("appName", "instanceName"))
                                                                 .orderBy(OrderBy.builder().name("timestamp").order(Order.asc).build())
                                                                 .dataSource(schema)
                                                                 .buildPipeline();

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
                              GROUP BY "appName", "instanceName", "activeThreads", "totalThreads"
                            ) AS "tbl2"
                            ORDER BY "timestamp" asc
                            """.trim(),
                            sqlGenerator.getSQL());
    }
}
