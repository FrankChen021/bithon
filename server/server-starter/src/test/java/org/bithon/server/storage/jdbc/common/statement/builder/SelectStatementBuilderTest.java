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

package org.bithon.server.storage.jdbc.common.statement.builder;

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.DefaultSchema;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.column.ExpressionColumn;
import org.bithon.server.datasource.column.StringColumn;
import org.bithon.server.datasource.column.aggregatable.last.AggregateLongLastColumn;
import org.bithon.server.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.datasource.expression.ExpressionASTBuilder;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.Order;
import org.bithon.server.datasource.query.OrderBy;
import org.bithon.server.datasource.query.ast.Alias;
import org.bithon.server.datasource.query.ast.ExpressionNode;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.bithon.server.datasource.reader.clickhouse.AggregateFunctionColumn;
import org.bithon.server.datasource.reader.clickhouse.ClickHouseSqlDialect;
import org.bithon.server.datasource.reader.h2.H2SqlDialect;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.ast.QueryStageFunctions;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;
import org.bithon.server.datasource.reader.jdbc.statement.builder.SelectStatementBuilder;
import org.bithon.server.datasource.reader.mysql.MySQLSqlDialect;
import org.bithon.server.datasource.reader.postgresql.PostgreSqlDialect;
import org.bithon.server.datasource.store.IDataStoreSpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * The test cases are located in this module because it has dependencies on concrete DB implementations.
 * And server-starter is the right module that depends on all DB implementations.
 *
 * @author frank.chen021@outlook.com
 * @date 26/7/24 10:44 am
 */
public class SelectStatementBuilderTest {

    private final ISchema schema = new DefaultSchema("bithon-http-incoming-metrics",
                                                     "bithon-http-incoming-metrics",
                                                     new TimestampSpec("timestamp"),
                                                     Arrays.asList(new StringColumn("appName", "appName"),
                                                                   new StringColumn("instance", "instance")),
                                                     Arrays.asList(new AggregateLongSumColumn("responseTime", "responseTime"),
                                                                   new AggregateLongSumColumn("totalCount", "totalCount"),
                                                                   new AggregateLongSumColumn("count4xx", "count4xx"),
                                                                   new AggregateLongSumColumn("count5xx", "count5xx"),
                                                                   new AggregateLongLastColumn("activeThreads", "activeThreads"),
                                                                   new AggregateLongLastColumn("totalThreads", "totalThreads"),
                                                                   new ExpressionColumn("avgResponseTime",
                                                                                        null,
                                                                                        "sum(responseTime) / sum(totalCount)",
                                                                                        "double"),

                                                                   // For ClickHouse data source
                                                                   new AggregateFunctionColumn("clickedSum", "clickedSum", "sum", IDataType.LONG),
                                                                   new AggregateFunctionColumn("clickedCnt", "clickedCnt", "count", IDataType.LONG)
                                                     ),
                                                     null,
                                                     new IDataStoreSpec() {
                                                         @Override
                                                         public String getStore() {
                                                             return "bithon_http_incoming_metrics";
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

    final ISqlDialect h2Dialect = new H2SqlDialect();
    final ISqlDialect mysql = new MySQLSqlDialect();
    final ISqlDialect clickHouseDialect = new ClickHouseSqlDialect();

    @BeforeAll
    public static void setUp() {
        new QueryStageFunctions().afterPropertiesSet();

        // Set the TimeZone to ensure that the test results are consistent on different machines with different TimeZones
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8:00"));
    }

    @AfterAll
    public static void tearDown() {
        // Reset the TimeZone to the default
        TimeZone.setDefault(TimeZone.getDefault());
    }

    @Test
    public void testSimpleAggregation_GroupBy() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(schema, "sum(totalCount)"), new Alias("t"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           sum("totalCount") AS "t"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName"
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(2, selectStatement.getSelectorList().size());
        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());
        Assertions.assertEquals("t", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(1).getDataType());
    }

    @Test
    public void testExpressionInAggregation_GroupBy() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "sum(totalCount*2)"), new Alias("t"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           sum("totalCount" * 2) AS "t"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName"
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(2, selectStatement.getSelectorList().size());
        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());
        Assertions.assertEquals("t", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(1).getDataType());
    }

    @Test
    public void testSimpleAggregation_TimeSeries() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "sum(totalCount)"), new Alias("totalCount"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      new IdentifierExpression("timestamp")
                                                                ))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                           "appName",
                                           sum("totalCount") AS "totalCount"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName", "_timestamp"
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());
        Assertions.assertEquals("_timestamp", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.DATETIME_MILLI, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("totalCount", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testPostAggregation_GroupBy() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "sum(responseTime*2)/sum(totalCount)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "_var0" / "sum_totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("responseTime" * 2) AS "_var0",
                                             sum("totalCount") AS "sum_totalCount"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("avg", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testPostAggregation_GroupBy_NestedFunction() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "round(round(sum(responseTime)/sum(totalCount),2), 2)"),
                                                                                                               new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           round(round(CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "sum_responseTime" / "sum_totalCount" ) ELSE ( 0 ) END, 2), 2) AS "avg"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("responseTime") AS "sum_responseTime",
                                             sum("totalCount") AS "sum_totalCount"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("avg", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testPostAggregation_TimeSeries() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(schema, "sum(responseTime)/sum(totalCount)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      new IdentifierExpression("timestamp")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           "appName",
                                           "instanceName",
                                           CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "sum_responseTime" / "sum_totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                             "appName",
                                             "instanceName",
                                             sum("responseTime") AS "sum_responseTime",
                                             sum("totalCount") AS "sum_totalCount"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName", "_timestamp"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("_timestamp", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.DATETIME_MILLI, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("avg", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void testPostAggregation_TimeSeries_DifferentWindowAndInterval_HasGroupBy() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(schema, "sum(responseTime)/sum(totalCount)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:05.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:05.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      // 5 minutes window
                                                                                      HumanReadableDuration.parse("5m"),
                                                                                      new IdentifierExpression("timestamp")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           "appName",
                                           "instanceName",
                                           CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "sum_responseTime" / "sum_totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT "_timestamp",
                                             "appName",
                                             "instanceName",
                                             sum("sum_responseTime") OVER (PARTITION BY "appName", "instanceName" ORDER BY "_timestamp" ASC RANGE BETWEEN 300 PRECEDING AND 0 FOLLOWING) AS "sum_responseTime",
                                             sum("sum_totalCount") OVER (PARTITION BY "appName", "instanceName" ORDER BY "_timestamp" ASC RANGE BETWEEN 300 PRECEDING AND 0 FOLLOWING) AS "sum_totalCount"
                                      FROM
                                      (
                                        SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                               "appName",
                                               "instanceName",
                                               sum("responseTime") AS "sum_responseTime",
                                               sum("totalCount") AS "sum_totalCount"
                                        FROM "bithon_http_incoming_metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:17:05.000+08:00') AND ("timestamp" < '2024-07-26T21:32:05.000+08:00')
                                        GROUP BY "appName", "instanceName", "_timestamp"
                                      )
                                      WHERE ("_timestamp" >= 1722000120) AND ("_timestamp" < 1722000725)
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("_timestamp", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.DATETIME_MILLI, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("avg", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void testPostAggregation_TimeSeries_DifferentWindowAndInterval_NoGroupBy() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(schema, "sum(responseTime)/sum(totalCount)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:05.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:05.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      // 5 minutes window
                                                                                      HumanReadableDuration.parse("5m"),
                                                                                      new IdentifierExpression("timestamp")))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "sum_responseTime" / "sum_totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT "_timestamp",
                                             sum("sum_responseTime") OVER (ORDER BY "_timestamp" ASC RANGE BETWEEN 300 PRECEDING AND 0 FOLLOWING) AS "sum_responseTime",
                                             sum("sum_totalCount") OVER (ORDER BY "_timestamp" ASC RANGE BETWEEN 300 PRECEDING AND 0 FOLLOWING) AS "sum_totalCount"
                                      FROM
                                      (
                                        SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                               sum("responseTime") AS "sum_responseTime",
                                               sum("totalCount") AS "sum_totalCount"
                                        FROM "bithon_http_incoming_metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:17:05.000+08:00') AND ("timestamp" < '2024-07-26T21:32:05.000+08:00')
                                        GROUP BY "_timestamp"
                                      )
                                      WHERE ("_timestamp" >= 1722000120) AND ("_timestamp" < 1722000725)
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(2, selectStatement.getSelectorList().size());

        Assertions.assertEquals("_timestamp", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.DATETIME_MILLI, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("avg", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(1).getDataType());
    }

    @Test
    public void testPostFunctionExpression_GroupBy() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(schema, "round(sum(responseTime)/sum(totalCount), 2)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           round(CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "sum_responseTime" / "sum_totalCount" ) ELSE ( 0 ) END, 2) AS "avg"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("responseTime") AS "sum_responseTime",
                                             sum("totalCount") AS "sum_totalCount"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("avg", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testDuplicateAggregations() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(List.of(new Selector(new ExpressionNode(schema,
                                                                                                                "sum(count4xx) + sum(count5xx)"),
                                                                                             new Alias("errorCount")),
                                                                                new Selector(new ExpressionNode(schema,
                                                                                                                "round((sum(count4xx) + sum(count5xx))*100.0/sum(totalCount), 2)"),
                                                                                             new Alias("errorRate"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           "sum_count4xx" + "sum_count5xx" AS "errorCount",
                                           round(CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( (("sum_count4xx" + "sum_count5xx") * 100.0) / "sum_totalCount" ) ELSE ( 0 ) END, 2) AS "errorRate"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("count4xx") AS "sum_count4xx",
                                             sum("count5xx") AS "sum_count5xx",
                                             sum("totalCount") AS "sum_totalCount"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("errorCount", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("errorRate", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void testWindowFunction_GroupBy() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "first(activeThreads)"), new Alias("a"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .filter(new ComparisonExpression.GT(new IdentifierExpression(
                                                                    "a"), new LiteralExpression.LongLiteral(5)))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           "a"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             FIRST_VALUE("activeThreads") OVER (PARTITION BY (UNIX_TIMESTAMP("timestamp") / 600) * 600 ORDER BY "timestamp" ASC) AS "a"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    )
                                    GROUP BY "appName", "instanceName", "a"
                                    HAVING "a" > 5
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("a", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testWindowFunction_GroupBy_NoUseWindowAggregator_CK() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(clickHouseDialect)
                                                                .fields(List.of(
                                                                    new Selector(new ExpressionNode(schema, "first(activeThreads)"), new Alias("a")),
                                                                    new Selector(new ExpressionNode(schema, "last(activeThreads)"), new Alias("b")))
                                                                )
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           argMin("activeThreads", "timestamp") AS "a",
                                           argMax("activeThreads", "timestamp") AS "b"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                    GROUP BY "appName", "instanceName"
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("a", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("b", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void testWindowFunction_GroupBy_NoUseWindowAggregator_UseTimeFilterGranularity_CK() {
        int[] granularities = new int[]{60, 60 * 5, 60 * 15, 3600, 86400};
        String[] functions = new String[]{"toStartOfMinute", "toStartOfFiveMinutes", "toStartOfFifteenMinutes", "toStartOfHour", "toStartOfDay"};

        for (int i = 0; i < granularities.length; i++) {
            int granularity = granularities[i];
            String function = functions[i];

            SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                    .sqlDialect(clickHouseDialect)
                                                                    .fields(List.of(
                                                                        new Selector(new ExpressionNode(schema, "first(activeThreads)"), new Alias("a")),
                                                                        new Selector(new ExpressionNode(schema, "last(activeThreads)"), new Alias("b")))
                                                                    )
                                                                    .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                          TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                    .groupBy(List.of("appName", "instanceName"))
                                                                    .schema(schema)
                                                                    // Set the time filter granularity to 60 seconds
                                                                    .querySettings(QuerySettings.builder()
                                                                                                .floorTimestampFilterGranularity(granularity)
                                                                                                .build())
                                                                    .build();

            Assertions.assertEquals(StringUtils.format("""
                                                           SELECT "appName",
                                                                  "instanceName",
                                                                  argMin("activeThreads", "timestamp") AS "a",
                                                                  argMax("activeThreads", "timestamp") AS "b"
                                                           FROM "bithon_http_incoming_metrics"
                                                           WHERE (%s("timestamp") >= fromUnixTimestamp(1722000120)) AND (%s("timestamp") < fromUnixTimestamp(1722000720))
                                                           GROUP BY "appName", "instanceName"
                                                           """.trim(),
                                                       function,
                                                       function),
                                    selectStatement.toSQL(clickHouseDialect));
        }
    }

    @Test
    public void testWindowFunction_TimeSeries() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "first(activeThreads)"),
                                                                                                               new Alias(
                                                                                                                   "activeThreads"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      new IdentifierExpression(
                                                                                          "timestamp")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           "appName",
                                           "instanceName",
                                           "activeThreads"
                                    FROM
                                    (
                                      SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                             "appName",
                                             "instanceName",
                                             FIRST_VALUE("activeThreads") OVER (PARTITION BY (UNIX_TIMESTAMP("timestamp") / 600) * 600 ORDER BY "timestamp" ASC) AS "activeThreads"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    )
                                    GROUP BY "appName", "instanceName", "activeThreads", "_timestamp"
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("_timestamp", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.DATETIME_MILLI, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("activeThreads", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void testWindowFunction_WithAggregator_H2() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "first(activeThreads)/sum(totalThreads)"), new Alias("ratio"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           CASE WHEN ( "sum_totalThreads" <> 0 ) THEN ( "first_activeThreads" / "sum_totalThreads" ) ELSE ( 0 ) END AS "ratio"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             "first_activeThreads",
                                             sum("totalThreads") AS "sum_totalThreads"
                                      FROM
                                      (
                                        SELECT "appName",
                                               "instanceName",
                                               FIRST_VALUE("activeThreads") OVER (PARTITION BY (UNIX_TIMESTAMP("timestamp") / 600) * 600 ORDER BY "timestamp" ASC) AS "first_activeThreads",
                                               "totalThreads"
                                        FROM "bithon_http_incoming_metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      )
                                      GROUP BY "appName", "instanceName", "first_activeThreads"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("ratio", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testWindowFunction_WithAggregator_CK() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(clickHouseDialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(schema, "first(activeThreads)/sum(totalThreads)"), new Alias("ratio"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           "first_activeThreads" / "sum_totalThreads" AS "ratio"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             argMin("activeThreads", "timestamp") AS "first_activeThreads",
                                             sum("totalThreads") AS "sum_totalThreads"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                      GROUP BY "appName", "instanceName"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("ratio", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testWindowFunctionAfterAggregator() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "sum(totalThreads) - first(activeThreads)"),
                                                                                                               new Alias(
                                                                                                                   "daemon"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           "sum_totalThreads" - "first_activeThreads" AS "daemon"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("totalThreads") AS "sum_totalThreads",
                                             "first_activeThreads"
                                      FROM
                                      (
                                        SELECT "appName",
                                               "instanceName",
                                               FIRST_VALUE("activeThreads") OVER (PARTITION BY (UNIX_TIMESTAMP("timestamp") / 600) * 600 ORDER BY "timestamp" ASC) AS "first_activeThreads",
                                               "totalThreads"
                                        FROM "bithon_http_incoming_metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      )
                                      GROUP BY "appName", "instanceName", "first_activeThreads"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("daemon", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testWindowFunctionAfterAggregator_MySQL() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(mysql)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(schema, "sum(totalThreads) - first(activeThreads)"), new Alias("daemon"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT `appName`,
                                           `instanceName`,
                                           `sum_totalThreads` - `first_activeThreads` AS `daemon`
                                    FROM
                                    (
                                      SELECT `appName`,
                                             `instanceName`,
                                             sum(`totalThreads`) AS `sum_totalThreads`,
                                             `first_activeThreads`
                                      FROM
                                      (
                                        SELECT `appName`,
                                               `instanceName`,
                                               FIRST_VALUE(`activeThreads`) OVER (PARTITION BY (UNIX_TIMESTAMP(`timestamp`) DIV 600) * 600 ORDER BY `timestamp` ASC) AS `first_activeThreads`,
                                               `totalThreads`
                                        FROM `bithon_http_incoming_metrics`
                                        WHERE (`timestamp` >= '2024-07-26T21:22:00.000+08:00') AND (`timestamp` < '2024-07-26T21:32:00.000+08:00')
                                      ) AS `tbl0`
                                      GROUP BY `appName`, `instanceName`, `first_activeThreads`
                                    ) AS `tbl1`
                                    """.trim(),
                                selectStatement.toSQL(mysql));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("daemon", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testAggregationWithMacroExpression() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Arrays.asList(
                                                                    new Selector(new ExpressionNode(schema, "sum(totalCount)/{interval}"), new Alias("qps")),
                                                                    new Selector(new ExpressionNode(schema, "sum(totalCount)/{instanceCount}"), new Alias("qpsPerInstance"))
                                                                ))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      new IdentifierExpression("timestamp")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           "appName",
                                           "instanceName",
                                           "sum_totalCount" / 10 AS "qps",
                                           CASE WHEN ( "cardinality_var0" <> 0 ) THEN ( "sum_totalCount" / "cardinality_var0" ) ELSE ( 0 ) END AS "qpsPerInstance"
                                    FROM
                                    (
                                      SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                             "appName",
                                             "instanceName",
                                             sum("totalCount") AS "sum_totalCount",
                                             count(distinct "instanceName") AS "cardinality_var0"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName", "_timestamp"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(5, selectStatement.getSelectorList().size());

        Assertions.assertEquals("_timestamp", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.DATETIME_MILLI, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("qps", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(3).getDataType());

        Assertions.assertEquals("qpsPerInstance", selectStatement.getSelectorList().get(4).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(4).getDataType());
    }

    @Test
    public void testCardinalityAggregation() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "cardinality(instance)"), new Alias("instanceCount"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .orderBy(OrderBy.builder()
                                                                                .name("appName")
                                                                                .order(Order.asc)
                                                                                .build())
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           count(distinct "instance") AS "instanceCount"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName"
                                    ORDER BY "appName" asc
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(2, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceCount", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(1).getDataType());
    }

    @Test
    public void testCountAggregation() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "count(1)"), new Alias("cnt"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .orderBy(OrderBy.builder()
                                                                                .name("appName")
                                                                                .order(Order.asc)
                                                                                .build())
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           count(1) AS "cnt"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName"
                                    ORDER BY "appName" asc
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(2, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("cnt", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(1).getDataType());
    }

    @Test
    public void testHumanReadableLiteral() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "count(1)"), new Alias("cnt"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .filter(new LogicalExpression.AND(new ComparisonExpression.GT(new IdentifierExpression("totalCount"),
                                                                                                                              new LiteralExpression.ReadableNumberLiteral(HumanReadableNumber.of("1MiB"))),
                                                                                                  new ComparisonExpression.LT(
                                                                                                      new IdentifierExpression("totalCount"),
                                                                                                      new LiteralExpression.ReadableDurationLiteral(
                                                                                                          HumanReadableDuration.parse("1h"))),
                                                                                                  new ComparisonExpression.LT(
                                                                                                      new IdentifierExpression("totalCount"),
                                                                                                      new LiteralExpression.ReadablePercentageLiteral(
                                                                                                          HumanReadablePercentage.of("50%")))
                                                                ))
                                                                .groupBy(List.of("appName"))
                                                                .orderBy(OrderBy.builder()
                                                                                .name("appName")
                                                                                .order(Order.asc)
                                                                                .build())
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           count(1) AS "cnt"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND (("bithon_http_incoming_metrics"."totalCount" > 1048576) AND ("bithon_http_incoming_metrics"."totalCount" < 3600) AND ("bithon_http_incoming_metrics"."totalCount" < 0.5))
                                    GROUP BY "appName"
                                    ORDER BY "appName" asc
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(2, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("cnt", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(1).getDataType());
    }

    /**
     * The filter is based on an expression
     */
    @Test
    public void testPostFilter_UseExpressionVariable() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(schema, "sum(responseTime*2)/sum(totalCount)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .filter(
                                                                    new LogicalExpression.AND(
                                                                        new ComparisonExpression.EQ(new IdentifierExpression("appName"),
                                                                                                    new LiteralExpression.StringLiteral("bithon")),
                                                                        new ComparisonExpression.GT(new IdentifierExpression("avg"),
                                                                                                    new LiteralExpression.DoubleLiteral(0.2))
                                                                    )
                                                                )
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "_var0" / "sum_totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("responseTime" * 2) AS "_var0",
                                             sum("totalCount") AS "sum_totalCount"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("bithon_http_incoming_metrics"."appName" = 'bithon')
                                      GROUP BY "appName", "instanceName"
                                    )
                                    WHERE "avg" > 0.2
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("avg", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(2).getDataType());
    }

    /**
     * The filter is based on an expression
     */
    @Test
    public void testPostFilter_UseAggregationAliasInFilter() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "sum(totalCount)"), new Alias("cnt"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .filter(new ComparisonExpression.GT(new IdentifierExpression(
                                                                    "cnt"), new LiteralExpression.LongLiteral(1000)))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           sum("totalCount") AS "cnt"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName", "instanceName"
                                    HAVING "cnt" > 1000
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("cnt", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());
    }

    /**
     * The filter is based on an expression
     */
    @Test
    public void testPostFilter_UseAggregationFilter() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "sum(totalCount)"), new Alias("totalCount"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .filter(new ComparisonExpression.GT(new IdentifierExpression("totalCount"),
                                                                                                    new LiteralExpression.LongLiteral(1000)))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           sum("totalCount") AS "totalCount"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName", "instanceName"
                                    HAVING "totalCount" > 1000
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("totalCount", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testPostFilter_FilterNotInTheSelectList_H2() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "sum(totalCount)"), new Alias("totalCount"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:32:00.000+0800")))
                                                                .filter(ExpressionASTBuilder.builder()
                                                                                            .schema(schema)
                                                                                            .functions(Functions.getInstance())
                                                                                            .build("avgResponseTime > 5"))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        // NOTE that in the WHERE clause, the rhs is 5.0, however, our input is 5
        // This is because the avgResponse is defined as DOUBLE,
        // and there's a type conversion in 'ExpressionTypeValidator'
        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           "sum_totalCount" AS "totalCount",
                                           CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "sum_responseTime" / "sum_totalCount" ) ELSE ( 0 ) END AS "avgResponseTime"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("totalCount") AS "sum_totalCount",
                                             sum("responseTime") AS "sum_responseTime"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName"
                                    )
                                    WHERE "avgResponseTime" > 5.0
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("totalCount", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("avgResponseTime", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void testPostFilter_FilterNotInTheSelectList_CK() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(clickHouseDialect)
                                                                .fields(Collections.singletonList(new Selector(new ExpressionNode(
                                                                    schema,
                                                                    "sum(totalCount)"), new Alias("totalCount"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:32:00.000+0800")))
                                                                .filter(ExpressionASTBuilder.builder()
                                                                                            .schema(schema)
                                                                                            .functions(Functions.getInstance())
                                                                                            .build("avgResponseTime > 5"))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        // NOTE that in the WHERE clause, the rhs is 5.0, however, our input is 5
        // This is because the avgResponse is defined as DOUBLE,
        // and there's a type conversion in 'ExpressionTypeValidator'
        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           "sum_totalCount" AS "totalCount",
                                           "sum_responseTime" / "sum_totalCount" AS "avgResponseTime"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("totalCount") AS "sum_totalCount",
                                             sum("responseTime") AS "sum_responseTime"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                      GROUP BY "appName", "instanceName"
                                    )
                                    WHERE "avgResponseTime" > 5.0
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instanceName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("totalCount", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("avgResponseTime", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void testAggregateFunctionColumn_CK() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(clickHouseDialect)
                                                                .fields(Arrays.asList(new Selector(new ExpressionNode(schema, "sum(clickedSum)"), new Alias("t1")),
                                                                                      new Selector(new ExpressionNode(schema, "count(clickedCnt)"), new Alias("t2"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           sumMerge("clickedSum") AS "t1",
                                           countMerge("clickedCnt") AS "t2"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                    GROUP BY "appName"
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("t1", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("t2", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testCountOverCountMergeColumn_CK() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(clickHouseDialect)
                                                                .fields(List.of(new Selector(new ExpressionNode(schema, "count(clickedSum)"), new Alias("t1"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           count("clickedSum") AS "t1"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                    GROUP BY "appName"
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(2, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("t1", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(1).getDataType());
    }

    @Test
    public void testSumOverCountMergeColumn_CK() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(clickHouseDialect)
                                                                .fields(List.of(new Selector(new ExpressionNode(schema, "sum(clickedCnt)"), new Alias("t1"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           sum(countMerge("clickedCnt")) AS "t1"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                    GROUP BY "appName"
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(2, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("t1", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(1).getDataType());
    }

    @Test
    public void test_SlidingWindowOverAggregateFunctionColumn_CK() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(clickHouseDialect)
                                                                .fields(Arrays.asList(new Selector(new ExpressionNode(schema, "sum(clickedSum)"), new Alias("t1")),
                                                                                      new Selector(new ExpressionNode(schema, "count(clickedCnt)"), new Alias("t2"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      HumanReadableDuration.parse("1m"),
                                                                                      new IdentifierExpression("timestamp")))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();
        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           "appName",
                                           sum("t1") OVER (PARTITION BY "appName" ORDER BY "_timestamp" ASC RANGE BETWEEN 60 PRECEDING AND 0 FOLLOWING) AS "t1",
                                           count("t2") OVER (PARTITION BY "appName" ORDER BY "_timestamp" ASC RANGE BETWEEN 60 PRECEDING AND 0 FOLLOWING) AS "t2"
                                    FROM
                                    (
                                      SELECT toUnixTimestamp(toStartOfInterval("timestamp", INTERVAL 10 SECOND)) AS "_timestamp",
                                             "appName",
                                             sumMerge("clickedSum") AS "t1",
                                             countMerge("clickedCnt") AS "t2"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= fromUnixTimestamp(1722000060)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                      GROUP BY "appName", "_timestamp"
                                    )
                                    WHERE ("_timestamp" >= 1722000120) AND ("_timestamp" < 1722000720)
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("_timestamp", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.DATETIME_MILLI, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("t1", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("t2", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void test_SlidingWindowAndOffset_CK() {
        // 1722000120
        TimeSpan currentStart = TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800");

        // 1722000720
        TimeSpan currentEnd = TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800");

        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(clickHouseDialect)
                                                                .fields(Arrays.asList(new Selector(new ExpressionNode(schema, "sum(clickedSum)"), new Alias("t1")),
                                                                                      new Selector(new ExpressionNode(schema, "count(clickedCnt)"), new Alias("t2"))))
                                                                .interval(Interval.of(currentStart,
                                                                                      currentEnd,
                                                                                      Duration.ofSeconds(10),
                                                                                      HumanReadableDuration.parse("1m"),
                                                                                      new IdentifierExpression("timestamp")))
                                                                // -1d offset
                                                                .offset(HumanReadableDuration.parse("-1d"))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        // The timestamp in INNER sub query which extends the query range by one duration which is 1m in this case
        TimeSpan innerStart = currentStart.before(Duration.ofDays(1)).before(Duration.ofMinutes(1));
        TimeSpan innerEnd = currentEnd.before(Duration.ofDays(1));

        // The inner query also offset the timestamp by 1d,
        // so for outer sub query, we need to remove records that fall in the extended range by applying a filter on the timestamp
        TimeSpan windowStart = innerStart.after(Duration.ofDays(1)).after(Duration.ofMinutes(1));
        TimeSpan windowEnd = innerEnd.after(Duration.ofDays(1));
        Assertions.assertEquals(StringUtils.format("""
                                                       SELECT "_timestamp",
                                                              "appName",
                                                              sum("t1") OVER (PARTITION BY "appName" ORDER BY "_timestamp" ASC RANGE BETWEEN 60 PRECEDING AND 0 FOLLOWING) AS "t1",
                                                              count("t2") OVER (PARTITION BY "appName" ORDER BY "_timestamp" ASC RANGE BETWEEN 60 PRECEDING AND 0 FOLLOWING) AS "t2"
                                                       FROM
                                                       (
                                                         SELECT toUnixTimestamp(toStartOfInterval("timestamp", INTERVAL 10 SECOND)) + 86400 AS "_timestamp",
                                                                "appName",
                                                                sumMerge("clickedSum") AS "t1",
                                                                countMerge("clickedCnt") AS "t2"
                                                         FROM "bithon_http_incoming_metrics"
                                                         WHERE ("timestamp" >= fromUnixTimestamp(%d)) AND ("timestamp" < fromUnixTimestamp(%d))
                                                         GROUP BY "appName", "_timestamp"
                                                       )
                                                       WHERE ("_timestamp" >= %d) AND ("_timestamp" < %d)
                                                       """.trim(),
                                                   innerStart.getSeconds(),
                                                   innerEnd.getSeconds(),
                                                   windowStart.getSeconds(),
                                                   windowEnd.getSeconds()
                                ),
                                selectStatement.toSQL(clickHouseDialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("_timestamp", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.DATETIME_MILLI, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("t1", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("t2", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void test_RegularExpressionMatch_H2() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(List.of(new Selector(new ExpressionNode(schema, "sum(clickedSum)"), new Alias("t1"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      null,
                                                                                      new IdentifierExpression("timestamp")))
                                                                .filter(ExpressionASTBuilder.builder().build("appName =~ 'bithon.*' AND instanceName !~ '192.*'"))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                           "appName",
                                           sum("clickedSum") AS "t1"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND (regexp_like("bithon_http_incoming_metrics"."appName", 'bithon.*', 'nm') AND (NOT regexp_like("bithon_http_incoming_metrics"."instanceName", '192.*', 'nm')))
                                    GROUP BY "appName", "_timestamp"
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));
    }

    @Test
    public void test_RegularExpressionMatch_CK() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(clickHouseDialect)
                                                                .fields(List.of(new Selector(new ExpressionNode(schema, "sum(clickedSum)"), new Alias("t1"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      null,
                                                                                      new IdentifierExpression("timestamp")))
                                                                .filter(ExpressionASTBuilder.builder().build("appName =~ 'bithon.*' AND instanceName !~ '192.*'"))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT toUnixTimestamp(toStartOfInterval("timestamp", INTERVAL 10 SECOND)) AS "_timestamp",
                                           "appName",
                                           sumMerge("clickedSum") AS "t1"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720)) AND (match("bithon_http_incoming_metrics"."appName", 'bithon.*') AND (NOT match("bithon_http_incoming_metrics"."instanceName", '192.*')))
                                    GROUP BY "appName", "_timestamp"
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));
    }

    @Test
    public void test_RegularExpressionMatch_Optimized_CK() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(clickHouseDialect)
                                                                .fields(List.of(new Selector(new ExpressionNode(schema, "sum(clickedSum)"), new Alias("t1"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      null,
                                                                                      new IdentifierExpression("timestamp")))
                                                                .filter(ExpressionASTBuilder.builder().build("appName =~ '^bithon.*' AND instanceName !~ '^192.*'"))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT toUnixTimestamp(toStartOfInterval("timestamp", INTERVAL 10 SECOND)) AS "_timestamp",
                                           "appName",
                                           sumMerge("clickedSum") AS "t1"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720)) AND (startsWith("bithon_http_incoming_metrics"."appName", 'bithon') AND (NOT startsWith("bithon_http_incoming_metrics"."instanceName", '192')))
                                    GROUP BY "appName", "_timestamp"
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));
    }

    @Test
    public void test_RegularExpressionMatch_MySQL() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(mysql)
                                                                .fields(List.of(new Selector(new ExpressionNode(schema, "sum(clickedSum)"), new Alias("t1"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      null,
                                                                                      new IdentifierExpression("timestamp")))
                                                                .filter(ExpressionASTBuilder.builder().build("appName =~ 'bithon.*' AND instanceName !~ '192.*'"))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT UNIX_TIMESTAMP(`timestamp`) div 10 * 10 AS `_timestamp`,
                                           `appName`,
                                           sum(`clickedSum`) AS `t1`
                                    FROM `bithon_http_incoming_metrics`
                                    WHERE (`timestamp` >= '2024-07-26T21:22:00.000+08:00') AND (`timestamp` < '2024-07-26T21:32:00.000+08:00') AND (REGEXP_LIKE(`bithon_http_incoming_metrics`.`appName`, 'bithon.*') AND (NOT REGEXP_LIKE(`bithon_http_incoming_metrics`.`instanceName`, '192.*')))
                                    GROUP BY `appName`, `_timestamp`
                                    """.trim(),
                                selectStatement.toSQL(mysql));
    }

    @Test
    public void test_RegularExpressionMatch_PG() {
        ISqlDialect pg = new PostgreSqlDialect();
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(pg)
                                                                .fields(List.of(new Selector(new ExpressionNode(schema, "sum(clickedSum)"), new Alias("t1"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      null,
                                                                                      new IdentifierExpression("timestamp")))
                                                                .filter(ExpressionASTBuilder.builder().build("appName =~ 'bithon.*' AND instanceName !~ '192.*'"))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT  FLOOR(EXTRACT(EPOCH FROM "timestamp" AT TIME ZONE 'UTC-8') / 10) * 10 AS "_timestamp",
                                           "appName",
                                           sum("clickedSum") AS "t1"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND (("bithon_http_incoming_metrics"."appName" ~ 'bithon.*') AND (NOT ("bithon_http_incoming_metrics"."instanceName" ~ '192.*')))
                                    GROUP BY "appName", "_timestamp"
                                    """.trim(),
                                selectStatement.toSQL(pg));
    }

    /**
     * <a href="https://github.com/FrankChen021/bithon/issues/1104">See the bug</a>
     */
    @Test
    public void test_MultipleAggregator_On_Same_Column() {
        ISqlDialect pg = new PostgreSqlDialect();
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(pg)
                                                                .fields(List.of(
                                                                    new Selector(new ExpressionNode(schema, "max(responseTime)"), new Alias("maxResponseTime")),
                                                                    new Selector(new ExpressionNode(schema, "min(responseTime)"), new Alias("minResponseTime")),
                                                                    new Selector(new ExpressionNode(schema, "sum(responseTime)/count()"), new Alias("avgResponseTime")),
                                                                    new Selector(new ExpressionNode(schema, "count()"), new Alias("count"))
                                                                ))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      null,
                                                                                      new IdentifierExpression("timestamp")))
                                                                .filter(ExpressionASTBuilder.builder().build("appName =~ 'bithon.*' AND instanceName !~ '192.*'"))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           "appName",
                                           "max_responseTime" AS "maxResponseTime",
                                           "min_responseTime" AS "minResponseTime",
                                           CASE WHEN ( "_var0" <> 0 ) THEN ( "sum_responseTime" / "_var0" ) ELSE ( 0 ) END AS "avgResponseTime",
                                           "_var0" AS "count"
                                    FROM
                                    (
                                      SELECT  FLOOR(EXTRACT(EPOCH FROM "timestamp" AT TIME ZONE 'UTC-8') / 10) * 10 AS "_timestamp",
                                             "appName",
                                             max("responseTime") AS "max_responseTime",
                                             min("responseTime") AS "min_responseTime",
                                             sum("responseTime") AS "sum_responseTime",
                                             count(1) AS "_var0"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND (("bithon_http_incoming_metrics"."appName" ~ 'bithon.*') AND (NOT ("bithon_http_incoming_metrics"."instanceName" ~ '192.*')))
                                      GROUP BY "appName", "_timestamp"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(pg));
    }
}
