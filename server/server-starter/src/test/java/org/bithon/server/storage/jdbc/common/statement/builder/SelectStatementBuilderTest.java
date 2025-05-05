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
import org.bithon.server.datasource.query.ast.Expression;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.reader.clickhouse.AggregateFunctionColumn;
import org.bithon.server.datasource.reader.clickhouse.ClickHouseSqlDialect;
import org.bithon.server.datasource.reader.h2.H2SqlDialect;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.SqlGenerator;
import org.bithon.server.datasource.reader.jdbc.statement.ast.QueryStageFunctions;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;
import org.bithon.server.datasource.reader.jdbc.statement.builder.SelectStatementBuilder;
import org.bithon.server.datasource.reader.mysql.MySQLSqlDialect;
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

    private final ISchema schema = new DefaultSchema("bithon-jvm-metrics",
                                                     "bithon-jvm-metrics",
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
                                                                .fields(Collections.singletonList(new Selector(new Expression(schema, "sum(totalCount)"), new Alias("t"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        // Generate and assert the SQL statement
        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           sum("totalCount") AS "t"
                                    FROM "bithon_jvm_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName"
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
                                                                    schema,
                                                                    "sum(totalCount*2)"), new Alias("t"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           sum("totalCount" * 2) AS "t"
                                    FROM "bithon_jvm_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName"
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
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

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                           "appName",
                                           sum("totalCount") AS "totalCount"
                                    FROM "bithon_jvm_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName", "_timestamp"
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
                                                                    schema,
                                                                    "sum(responseTime*2)/sum(totalCount)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           CASE WHEN ( "totalCount" <> 0 ) THEN ( "_var0" / "totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("responseTime" * 2) AS "_var0",
                                             sum("totalCount") AS "totalCount"
                                      FROM "bithon_jvm_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName"
                                    )
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
                                                                    schema,
                                                                    "round(round(sum(responseTime)/sum(totalCount),2), 2)"),
                                                                                                               new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           round(round(CASE WHEN ( "totalCount" <> 0 ) THEN ( "responseTime" / "totalCount" ) ELSE ( 0 ) END, 2), 2) AS "avg"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("responseTime") AS "responseTime",
                                             sum("totalCount") AS "totalCount"
                                      FROM "bithon_jvm_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName"
                                    )
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(schema, "sum(responseTime)/sum(totalCount)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      new IdentifierExpression("timestamp")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           "appName",
                                           "instanceName",
                                           CASE WHEN ( "totalCount" <> 0 ) THEN ( "responseTime" / "totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                             "appName",
                                             "instanceName",
                                             sum("responseTime") AS "responseTime",
                                             sum("totalCount") AS "totalCount"
                                      FROM "bithon_jvm_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName", "_timestamp"
                                    )
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(schema, "sum(responseTime)/sum(totalCount)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:05.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:05.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      // 5 minutes window
                                                                                      HumanReadableDuration.parse("5m"),
                                                                                      new IdentifierExpression("timestamp")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           "appName",
                                           "instanceName",
                                           CASE WHEN ( "totalCount" <> 0 ) THEN ( "responseTime" / "totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT "_timestamp",
                                             "appName",
                                             "instanceName",
                                             sum("responseTime") OVER (PARTITION BY "appName", "instanceName" ORDER BY "_timestamp" ASC RANGE BETWEEN 300 PRECEDING AND 0 FOLLOWING) AS "responseTime",
                                             sum("totalCount") OVER (PARTITION BY "appName", "instanceName" ORDER BY "_timestamp" ASC RANGE BETWEEN 300 PRECEDING AND 0 FOLLOWING) AS "totalCount"
                                      FROM
                                      (
                                        SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                               "appName",
                                               "instanceName",
                                               sum("responseTime") AS "responseTime",
                                               sum("totalCount") AS "totalCount"
                                        FROM "bithon_jvm_metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:17:05.000+08:00') AND ("timestamp" < '2024-07-26T21:32:05.000+08:00')
                                        GROUP BY "appName", "instanceName", "_timestamp"
                                      )
                                      WHERE ("_timestamp" >= 1722000120) AND ("_timestamp" < 1722000725)
                                    )
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(schema, "sum(responseTime)/sum(totalCount)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:05.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:05.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      // 5 minutes window
                                                                                      HumanReadableDuration.parse("5m"),
                                                                                      new IdentifierExpression("timestamp")))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           CASE WHEN ( "totalCount" <> 0 ) THEN ( "responseTime" / "totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT "_timestamp",
                                             sum("responseTime") OVER (ORDER BY "_timestamp" ASC RANGE BETWEEN 300 PRECEDING AND 0 FOLLOWING) AS "responseTime",
                                             sum("totalCount") OVER (ORDER BY "_timestamp" ASC RANGE BETWEEN 300 PRECEDING AND 0 FOLLOWING) AS "totalCount"
                                      FROM
                                      (
                                        SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                               sum("responseTime") AS "responseTime",
                                               sum("totalCount") AS "totalCount"
                                        FROM "bithon_jvm_metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:17:05.000+08:00') AND ("timestamp" < '2024-07-26T21:32:05.000+08:00')
                                        GROUP BY "_timestamp"
                                      )
                                      WHERE ("_timestamp" >= 1722000120) AND ("_timestamp" < 1722000725)
                                    )
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(schema, "round(sum(responseTime)/sum(totalCount), 2)"), new Alias("avg"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           round(CASE WHEN ( "totalCount" <> 0 ) THEN ( "responseTime" / "totalCount" ) ELSE ( 0 ) END, 2) AS "avg"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("responseTime") AS "responseTime",
                                             sum("totalCount") AS "totalCount"
                                      FROM "bithon_jvm_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName"
                                    )
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(List.of(new Selector(new Expression(schema,
                                                                                                            "sum(count4xx) + sum(count5xx)"),
                                                                                             new Alias("errorCount")),
                                                                                new Selector(new Expression(schema,
                                                                                                            "round((sum(count4xx) + sum(count5xx))*100.0/sum(totalCount), 2)"),
                                                                                             new Alias("errorRate"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           "count4xx" + "count5xx" AS "errorCount",
                                           round(CASE WHEN ( "totalCount" <> 0 ) THEN ( (("count4xx" + "count5xx") * 100.0) / "totalCount" ) ELSE ( 0 ) END, 2) AS "errorRate"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("count4xx") AS "count4xx",
                                             sum("count5xx") AS "count5xx",
                                             sum("totalCount") AS "totalCount"
                                      FROM "bithon_jvm_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName"
                                    )
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
                                                                    schema,
                                                                    "first(activeThreads)"), new Alias("a"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .filter(new ComparisonExpression.GT(new IdentifierExpression(
                                                                    "a"), new LiteralExpression.LongLiteral(5)))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           "a"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             FIRST_VALUE("activeThreads") OVER (PARTITION BY (UNIX_TIMESTAMP("timestamp") / 600) * 600 ORDER BY "timestamp" ASC) AS "a"
                                      FROM "bithon_jvm_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    )
                                    GROUP BY "appName", "instanceName", "a"
                                    HAVING "a" > 5
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                    new Selector(new Expression(schema, "first(activeThreads)"), new Alias("a")),
                                                                    new Selector(new Expression(schema, "last(activeThreads)"), new Alias("b")))
                                                                )
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(clickHouseDialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           argMin("activeThreads", "timestamp") AS "a",
                                           argMax("activeThreads", "timestamp") AS "b"
                                    FROM "bithon_jvm_metrics"
                                    WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                    GROUP BY "appName", "instanceName"
                                    """.trim(),
                                sqlGenerator.getSQL());

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
    public void testWindowFunction_TimeSeries() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(h2Dialect)
                                                                .fields(Collections.singletonList(new Selector(new Expression(
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

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

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
                                      FROM "bithon_jvm_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    )
                                    GROUP BY "appName", "instanceName", "activeThreads", "_timestamp"
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
                                                                    schema,
                                                                    "first(activeThreads)/sum(totalThreads)"), new Alias("ratio"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .orderBy(OrderBy.builder()
                                                                                .name("timestamp")
                                                                                .order(Order.asc)
                                                                                .build())
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           CASE WHEN ( "totalThreads" <> 0 ) THEN ( "activeThreads" / "totalThreads" ) ELSE ( 0 ) END AS "ratio"
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
                                               FIRST_VALUE("activeThreads") OVER (PARTITION BY (UNIX_TIMESTAMP("timestamp") / 600) * 600 ORDER BY "timestamp" ASC) AS "activeThreads",
                                               "totalThreads"
                                        FROM "bithon_jvm_metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      )
                                      GROUP BY "appName", "instanceName", "activeThreads"
                                    )
                                    ORDER BY "timestamp" asc
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(schema, "first(activeThreads)/sum(totalThreads)"), new Alias("ratio"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .orderBy(OrderBy.builder()
                                                                                .name("timestamp")
                                                                                .order(Order.asc)
                                                                                .build())
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(clickHouseDialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
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
                                      WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                      GROUP BY "appName", "instanceName"
                                    )
                                    ORDER BY "timestamp" asc
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
                                                                    schema,
                                                                    "sum(totalThreads) - first(activeThreads)"),
                                                                                                               new Alias(
                                                                                                                   "daemon"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .orderBy(OrderBy.builder()
                                                                                .name("timestamp")
                                                                                .order(Order.asc)
                                                                                .build())
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
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
                                               FIRST_VALUE("activeThreads") OVER (PARTITION BY (UNIX_TIMESTAMP("timestamp") / 600) * 600 ORDER BY "timestamp" ASC) AS "activeThreads",
                                               "totalThreads"
                                        FROM "bithon_jvm_metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      )
                                      GROUP BY "appName", "instanceName", "activeThreads"
                                    )
                                    ORDER BY "timestamp" asc
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
                                                                    schema,
                                                                    "sum(totalThreads) - first(activeThreads)"), new Alias("daemon"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .orderBy(OrderBy.builder()
                                                                                .name("timestamp")
                                                                                .order(Order.asc)
                                                                                .build())
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(mysql);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT `appName`,
                                           `instanceName`,
                                           `totalThreads` - `activeThreads` AS `daemon`
                                    FROM
                                    (
                                      SELECT `appName`,
                                             `instanceName`,
                                             sum(`totalThreads`) AS `totalThreads`,
                                             `activeThreads`
                                      FROM
                                      (
                                        SELECT `appName`,
                                               `instanceName`,
                                               FIRST_VALUE(`activeThreads`) OVER (PARTITION BY (UNIX_TIMESTAMP(`timestamp`) DIV 600) * 600 ORDER BY `timestamp` ASC) AS `activeThreads`,
                                               `totalThreads`
                                        FROM `bithon_jvm_metrics`
                                        WHERE (`timestamp` >= '2024-07-26T21:22:00.000+08:00') AND (`timestamp` < '2024-07-26T21:32:00.000+08:00')
                                      ) AS `tbl0`
                                      GROUP BY `appName`, `instanceName`, `activeThreads`
                                    ) AS `tbl1`
                                    ORDER BY `timestamp` asc
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                    new Selector(new Expression(schema, "sum(totalCount)/{interval}"), new Alias("qps")),
                                                                    new Selector(new Expression(schema, "sum(totalCount)/{instanceCount}"), new Alias("qpsPerInstance"))
                                                                ))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      new IdentifierExpression("timestamp")))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .orderBy(OrderBy.builder()
                                                                                .name("appName")
                                                                                .order(Order.asc)
                                                                                .build())
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           "appName",
                                           "instanceName",
                                           "totalCount" / 10 AS "qps",
                                           CASE WHEN ( "cardinality_var0" <> 0 ) THEN ( "totalCount" / "cardinality_var0" ) ELSE ( 0 ) END AS "qpsPerInstance"
                                    FROM
                                    (
                                      SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                             "appName",
                                             "instanceName",
                                             sum("totalCount") AS "totalCount",
                                             count(distinct "instanceName") AS "cardinality_var0"
                                      FROM "bithon_jvm_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName", "_timestamp"
                                    )
                                    ORDER BY "appName" asc
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
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

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           count(distinct "instance") AS "instanceCount"
                                    FROM "bithon_jvm_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName"
                                    ORDER BY "appName" asc
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
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

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           count(1) AS "cnt"
                                    FROM "bithon_jvm_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName"
                                    ORDER BY "appName" asc
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
                                                                    schema,
                                                                    "count(1)"), new Alias("cnt"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:32:00.000+0800")))
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

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           count(1) AS "cnt"
                                    FROM "bithon_jvm_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND (("bithon_jvm_metrics"."totalCount" > 1048576) AND ("bithon_jvm_metrics"."totalCount" < 3600) AND ("bithon_jvm_metrics"."totalCount" < 0.5))
                                    GROUP BY "appName"
                                    ORDER BY "appName" asc
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
                                                                    schema,
                                                                    "sum(responseTime*2)/sum(totalCount)"), new Alias("avg"))))
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

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           CASE WHEN ( "totalCount" <> 0 ) THEN ( "_var0" / "totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("responseTime" * 2) AS "_var0",
                                             sum("totalCount") AS "totalCount"
                                      FROM "bithon_jvm_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("bithon_jvm_metrics"."appName" = 'bithon')
                                      GROUP BY "appName", "instanceName"
                                    )
                                    WHERE "avg" > 0.2
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
                                                                    schema,
                                                                    "sum(totalCount)"), new Alias("cnt"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601(
                                                                                          "2024-07-26T21:32:00.000+0800")))
                                                                .filter(new ComparisonExpression.GT(new IdentifierExpression(
                                                                    "cnt"), new LiteralExpression.LongLiteral(1000)))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           sum("totalCount") AS "cnt"
                                    FROM "bithon_jvm_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName", "instanceName"
                                    HAVING "cnt" > 1000
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
                                                                    schema,
                                                                    "sum(totalCount)"), new Alias("totalCount"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .filter(new ComparisonExpression.GT(new IdentifierExpression("totalCount"),
                                                                                                    new LiteralExpression.LongLiteral(1000)))
                                                                .groupBy(List.of("appName", "instanceName"))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           sum("totalCount") AS "totalCount"
                                    FROM "bithon_jvm_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName", "instanceName"
                                    HAVING "totalCount" > 1000
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
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

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        // NOTE that in the WHERE clause, the rhs is 5.0, however, our input is 5
        // This is because the avgResponse is defined as DOUBLE,
        // and there's a type conversion in 'ExpressionTypeValidator'
        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           "totalCount",
                                           CASE WHEN ( "totalCount" <> 0 ) THEN ( "responseTime" / "totalCount" ) ELSE ( 0 ) END AS "avgResponseTime"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("totalCount") AS "totalCount",
                                             sum("responseTime") AS "responseTime"
                                      FROM "bithon_jvm_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instanceName"
                                    )
                                    WHERE "avgResponseTime" > 5.0
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Collections.singletonList(new Selector(new Expression(
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

        SqlGenerator sqlGenerator = new SqlGenerator(clickHouseDialect);
        sqlGenerator.generate(selectStatement);

        // NOTE that in the WHERE clause, the rhs is 5.0, however, our input is 5
        // This is because the avgResponse is defined as DOUBLE,
        // and there's a type conversion in 'ExpressionTypeValidator'
        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instanceName",
                                           "totalCount",
                                           "responseTime" / "totalCount" AS "avgResponseTime"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instanceName",
                                             sum("totalCount") AS "totalCount",
                                             sum("responseTime") AS "responseTime"
                                      FROM "bithon_jvm_metrics"
                                      WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                      GROUP BY "appName", "instanceName"
                                    )
                                    WHERE "avgResponseTime" > 5.0
                                    """.trim(),
                                sqlGenerator.getSQL());

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
                                                                .fields(Arrays.asList(new Selector(new Expression(schema, "sum(clickedSum)"), new Alias("t1")),
                                                                                      new Selector(new Expression(schema, "count(clickedCnt)"), new Alias("t2"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           sumMerge("clickedSum") AS "t1",
                                           countMerge("clickedCnt") AS "t2"
                                    FROM "bithon_jvm_metrics"
                                    WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                    GROUP BY "appName"
                                    """.trim(),
                                sqlGenerator.getSQL());

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
    public void test_SlidingWindowOverAggregateFunctionColumn_CK() {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .sqlDialect(clickHouseDialect)
                                                                .fields(Arrays.asList(new Selector(new Expression(schema, "sum(clickedSum)"), new Alias("t1")),
                                                                                      new Selector(new Expression(schema, "count(clickedCnt)"), new Alias("t2"))))
                                                                .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                                                                                      TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                                                                                      Duration.ofSeconds(10),
                                                                                      HumanReadableDuration.parse("1m"),
                                                                                      new IdentifierExpression("timestamp")))
                                                                .groupBy(List.of("appName"))
                                                                .schema(schema)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        sqlGenerator.generate(selectStatement);

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
                                      FROM "bithon_jvm_metrics"
                                      WHERE ("timestamp" >= fromUnixTimestamp(1722000060)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                      GROUP BY "appName", "_timestamp"
                                    )
                                    WHERE ("_timestamp" >= 1722000120) AND ("_timestamp" < 1722000720)
                                    """.trim(),
                                sqlGenerator.getSQL());

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

    /**
     * TODO: support in this case in future
     * A little complex case
     @Test public void testPostFilter_AggregationInFilter() {
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

     Assertions.assertEquals("""
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
     }*/
}
