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

import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.DefaultSchema;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.column.ExpressionColumn;
import org.bithon.server.datasource.column.StringColumn;
import org.bithon.server.datasource.column.aggregatable.last.AggregateLongLastColumn;
import org.bithon.server.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.query.Limit;
import org.bithon.server.datasource.query.Order;
import org.bithon.server.datasource.query.OrderBy;
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
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryField;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.impl.QueryConverter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
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
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("t", "totalCount", null, "sum(totalCount)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
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
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("t", "totalCount", null, "sum(totalCount*2)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
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
        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                         .step(10)

                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("totalCount", "totalCount", null, "sum(totalCount)")))
                                                .interval(intervalRequest)
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(h2Dialect)
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
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("avg", "responseTime", null, "sum(responseTime*2)/sum(totalCount)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "_var0" / "sum_totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instance",
                                             sum("responseTime" * 2) AS "_var0",
                                             sum("totalCount") AS "sum_totalCount"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instance"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("avg", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testPostAggregation_GroupBy_NestedFunction() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("avg", "responseTime", null, "round(round(sum(responseTime)/sum(totalCount),2), 2)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           round(round(CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "sum_responseTime" / "sum_totalCount" ) ELSE ( 0 ) END, 2), 2) AS "avg"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instance",
                                             sum("responseTime") AS "sum_responseTime",
                                             sum("totalCount") AS "sum_totalCount"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instance"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("avg", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testPostAggregation_TimeSeries() {
        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                         .step(10)

                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("avg", "responseTime", null, "sum(responseTime)/sum(totalCount)")))
                                                .interval(intervalRequest)
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           "appName",
                                           "instance",
                                           CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "sum_responseTime" / "sum_totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                             "appName",
                                             "instance",
                                             sum("responseTime") AS "sum_responseTime",
                                             sum("totalCount") AS "sum_totalCount"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instance", "_timestamp"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("_timestamp", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.DATETIME_MILLI, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("avg", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void testPostAggregation_TimeSeries_DifferentWindowAndInterval_HasGroupBy() {
        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:05.000+0800"))
                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:05.000+0800"))
                                                         .step(10)

                                                         .window(HumanReadableDuration.parse("5m"))
                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("avg", "responseTime", null, "sum(responseTime)/sum(totalCount)")))
                                                .interval(intervalRequest)
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           "appName",
                                           "instance",
                                           CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "sum_responseTime" / "sum_totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT "_timestamp",
                                             "appName",
                                             "instance",
                                             sum("sum_responseTime") OVER (PARTITION BY "appName", "instance" ORDER BY "_timestamp" ASC RANGE BETWEEN 300 PRECEDING AND 0 FOLLOWING) AS "sum_responseTime",
                                             sum("sum_totalCount") OVER (PARTITION BY "appName", "instance" ORDER BY "_timestamp" ASC RANGE BETWEEN 300 PRECEDING AND 0 FOLLOWING) AS "sum_totalCount"
                                      FROM
                                      (
                                        SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                               "appName",
                                               "instance",
                                               sum("responseTime") AS "sum_responseTime",
                                               sum("totalCount") AS "sum_totalCount"
                                        FROM "bithon_http_incoming_metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:17:05.000+08:00') AND ("timestamp" < '2024-07-26T21:32:05.000+08:00')
                                        GROUP BY "appName", "instance", "_timestamp"
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

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("avg", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void testPostAggregation_TimeSeries_DifferentWindowAndInterval_NoGroupBy() {
        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:05.000+0800"))
                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:05.000+0800"))
                                                         .step(10)

                                                         .window(HumanReadableDuration.parse("5m"))
                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("avg", "responseTime", null, "sum(responseTime)/sum(totalCount)")))
                                                .interval(intervalRequest)
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(h2Dialect)
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
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("avg", "responseTime", null, "round(sum(responseTime)/sum(totalCount), 2)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           round(CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "sum_responseTime" / "sum_totalCount" ) ELSE ( 0 ) END, 2) AS "avg"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instance",
                                             sum("responseTime") AS "sum_responseTime",
                                             sum("totalCount") AS "sum_totalCount"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instance"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("avg", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testDuplicateAggregations() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(
                                                    new QueryField("errorCount", "count4xx", null, "sum(count4xx) + sum(count5xx)"),
                                                    new QueryField("errorRate", "count4xx", null, "round((sum(count4xx) + sum(count5xx))*100.0/sum(totalCount), 2)")
                                                ))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           "sum_count4xx" + "sum_count5xx" AS "errorCount",
                                           round(CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( (("sum_count4xx" + "sum_count5xx") * 100.0) / "sum_totalCount" ) ELSE ( 0 ) END, 2) AS "errorRate"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instance",
                                             sum("count4xx") AS "sum_count4xx",
                                             sum("count5xx") AS "sum_count5xx",
                                             sum("totalCount") AS "sum_totalCount"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instance"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("errorCount", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("errorRate", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void testWindowFunction_GroupBy() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("a", "activeThreads", null, "first(activeThreads)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .filterExpression("a > 5")
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           "a"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instance",
                                             FIRST_VALUE("activeThreads") OVER (PARTITION BY (UNIX_TIMESTAMP("timestamp") / 600) * 600 ORDER BY "timestamp" ASC) AS "a"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    )
                                    GROUP BY "appName", "instance", "a"
                                    HAVING "a" > 5
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("a", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testWindowFunction_GroupBy_NoUseWindowAggregator_CK() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(
                                                    new QueryField("a", "activeThreads", null, "first(activeThreads)"),
                                                    new QueryField("b", "activeThreads", null, "last(activeThreads)")
                                                ))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(clickHouseDialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           argMin("activeThreads", "timestamp") AS "a",
                                           argMax("activeThreads", "timestamp") AS "b"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                    GROUP BY "appName", "instance"
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
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

            TimeSpan start = TimeSpan.fromISO8601("2024-07-26T21:22:03.000+0800");
            TimeSpan end = TimeSpan.fromISO8601("2024-07-26T21:32:04.000+0800");
            IntervalRequest intervalRequest = IntervalRequest.builder()
                                                             .startISO8601(start)
                                                             .endISO8601(end)
                                                             .build();
            QueryRequest queryRequest = QueryRequest.builder()
                                                    .fields(List.of(
                                                        new QueryField("a", "activeThreads", null, "first(activeThreads)"),
                                                        new QueryField("b", "activeThreads", null, "last(activeThreads)")
                                                    ))
                                                    .interval(intervalRequest)
                                                    .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                    .settings(QuerySettings.builder()
                                                                           .floorTimestampFilterGranularity(granularity)
                                                                           .build())
                                                    .build();
            SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                    .sqlDialect(clickHouseDialect)
                                                                    .build();

            Assertions.assertEquals(StringUtils.format("""
                                                           SELECT "appName",
                                                                  "instance",
                                                                  argMin("activeThreads", "timestamp") AS "a",
                                                                  argMax("activeThreads", "timestamp") AS "b"
                                                           FROM "bithon_http_incoming_metrics"
                                                           WHERE (%s("timestamp") >= fromUnixTimestamp(%d)) AND (%s("timestamp") < fromUnixTimestamp(%d))
                                                           GROUP BY "appName", "instance"
                                                           """.trim(),
                                                       function,
                                                       start.floor(Duration.ofSeconds(granularity)).getSeconds(),
                                                       function,
                                                       end.ceil(Duration.ofSeconds(granularity)).getSeconds()
                                    ),
                                    selectStatement.toSQL(clickHouseDialect));
        }
    }

    @Test
    public void testWindowFunction_TimeSeries() {
        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                         .step(10)

                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("activeThreads", "activeThreads", null, "first(activeThreads)")))
                                                .interval(intervalRequest)
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           "appName",
                                           "instance",
                                           "activeThreads"
                                    FROM
                                    (
                                      SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                             "appName",
                                             "instance",
                                             FIRST_VALUE("activeThreads") OVER (PARTITION BY (UNIX_TIMESTAMP("timestamp") / 600) * 600 ORDER BY "timestamp" ASC) AS "activeThreads"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    )
                                    GROUP BY "appName", "instance", "activeThreads", "_timestamp"
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("_timestamp", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.DATETIME_MILLI, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("activeThreads", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void testWindowFunction_WithAggregator_H2() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("ratio", "activeThreads", null, "first(activeThreads)/sum(totalThreads)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           CASE WHEN ( "sum_totalThreads" <> 0 ) THEN ( "first_activeThreads" / "sum_totalThreads" ) ELSE ( 0 ) END AS "ratio"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instance",
                                             "first_activeThreads",
                                             sum("totalThreads") AS "sum_totalThreads"
                                      FROM
                                      (
                                        SELECT "appName",
                                               "instance",
                                               FIRST_VALUE("activeThreads") OVER (PARTITION BY (UNIX_TIMESTAMP("timestamp") / 600) * 600 ORDER BY "timestamp" ASC) AS "first_activeThreads",
                                               "totalThreads"
                                        FROM "bithon_http_incoming_metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      )
                                      GROUP BY "appName", "instance", "first_activeThreads"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("ratio", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testWindowFunction_WithAggregator_CK() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("ratio", "activeThreads", null, "first(activeThreads)/sum(totalThreads)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(clickHouseDialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           "first_activeThreads" / "sum_totalThreads" AS "ratio"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instance",
                                             argMin("activeThreads", "timestamp") AS "first_activeThreads",
                                             sum("totalThreads") AS "sum_totalThreads"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                      GROUP BY "appName", "instance"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("ratio", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testWindowFunctionAfterAggregator() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("daemon", "totalThreads", null, "sum(totalThreads) - first(activeThreads)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           "sum_totalThreads" - "first_activeThreads" AS "daemon"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instance",
                                             sum("totalThreads") AS "sum_totalThreads",
                                             "first_activeThreads"
                                      FROM
                                      (
                                        SELECT "appName",
                                               "instance",
                                               FIRST_VALUE("activeThreads") OVER (PARTITION BY (UNIX_TIMESTAMP("timestamp") / 600) * 600 ORDER BY "timestamp" ASC) AS "first_activeThreads",
                                               "totalThreads"
                                        FROM "bithon_http_incoming_metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      )
                                      GROUP BY "appName", "instance", "first_activeThreads"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("daemon", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testWindowFunctionAfterAggregator_MySQL() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("daemon", "totalThreads", null, "sum(totalThreads) - first(activeThreads)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(mysql)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT `appName`,
                                           `instance`,
                                           `sum_totalThreads` - `first_activeThreads` AS `daemon`
                                    FROM
                                    (
                                      SELECT `appName`,
                                             `instance`,
                                             sum(`totalThreads`) AS `sum_totalThreads`,
                                             `first_activeThreads`
                                      FROM
                                      (
                                        SELECT `appName`,
                                               `instance`,
                                               FIRST_VALUE(`activeThreads`) OVER (PARTITION BY (UNIX_TIMESTAMP(`timestamp`) DIV 600) * 600 ORDER BY `timestamp` ASC) AS `first_activeThreads`,
                                               `totalThreads`
                                        FROM `bithon_http_incoming_metrics`
                                        WHERE (`timestamp` >= '2024-07-26T21:22:00.000+08:00') AND (`timestamp` < '2024-07-26T21:32:00.000+08:00')
                                      ) AS `tbl0`
                                      GROUP BY `appName`, `instance`, `first_activeThreads`
                                    ) AS `tbl1`
                                    """.trim(),
                                selectStatement.toSQL(mysql));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("daemon", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testAggregationWithMacroExpression() {
        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                         .step(10)

                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(
                                                    new QueryField("qps", "totalCount", null, "sum(totalCount)/{interval}"),
                                                    new QueryField("qpsPerInstance", "totalCount", null, "sum(totalCount)/cardinality(instance)")
                                                ))
                                                .interval(intervalRequest)
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "_timestamp",
                                           "appName",
                                           "instance",
                                           "sum_totalCount" / 10 AS "qps",
                                           CASE WHEN ( "cardinality_var0" <> 0 ) THEN ( "sum_totalCount" / "cardinality_var0" ) ELSE ( 0 ) END AS "qpsPerInstance"
                                    FROM
                                    (
                                      SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                             "appName",
                                             "instance",
                                             sum("totalCount") AS "sum_totalCount",
                                             count(distinct "instance") AS "cardinality_var0"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instance", "_timestamp"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(5, selectStatement.getSelectorList().size());

        Assertions.assertEquals("_timestamp", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.DATETIME_MILLI, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("qps", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(3).getDataType());

        Assertions.assertEquals("qpsPerInstance", selectStatement.getSelectorList().get(4).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(4).getDataType());
    }

    @Test
    public void testCardinalityAggregation() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("instanceCount", "instance", null, "cardinality(instance)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .orderBy(OrderBy.builder()
                                                                .name("appName")
                                                                .order(Order.asc)
                                                                .build())
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
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
    public void testCardinalityAggregation_RewriteToGroupBy_False() {
        // the default behavior is NOT to rewrite distinct count to group by
        QuerySettings settings = QuerySettings.builder()
                                              .rewriteCardinalityToGroupBy(false)
                                              .build();

        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("instanceCount", "instance", null, "cardinality(instance)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .settings(settings)
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT count(distinct "instance") AS "instanceCount"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(1, selectStatement.getSelectorList().size());

        Assertions.assertEquals("instanceCount", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(0).getDataType());
    }

    @Test
    public void testCardinalityAggregation_RewriteToGroupBy_True() {
        // the default behavior is NOT to rewrite distinct count to group by
        QuerySettings settings = QuerySettings.builder()
                                              .rewriteCardinalityToGroupBy(true)
                                              .build();

        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("instanceCount", "instance", null, "cardinality(instance)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .settings(settings)
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT count(1) AS "instanceCount"
                                    FROM
                                    (
                                      SELECT "instance" AS "instance"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "instance"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(1, selectStatement.getSelectorList().size());

        Assertions.assertEquals("instanceCount", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(0).getDataType());
    }

    @Test
    public void testCountAggregation() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("cnt", "1", null, "count(1)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .orderBy(OrderBy.builder()
                                                                .name("appName")
                                                                .order(Order.asc)
                                                                .build())
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
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
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("cnt", "1", null, "count(1)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .filterExpression("totalCount > 1MiB AND totalCount < 1h AND totalCount < 50%")
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .orderBy(OrderBy.builder()
                                                                .name("appName")
                                                                .order(Order.asc)
                                                                .build())
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
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
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("avg", "responseTime", null, "sum(responseTime*2)/sum(totalCount)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .filterExpression("appName = 'bithon' AND avg > 0.2")
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "_var0" / "sum_totalCount" ) ELSE ( 0 ) END AS "avg"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instance",
                                             sum("responseTime" * 2) AS "_var0",
                                             sum("totalCount") AS "sum_totalCount"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("bithon_http_incoming_metrics"."appName" = 'bithon')
                                      GROUP BY "appName", "instance"
                                    )
                                    WHERE "avg" > 0.2
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("avg", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(2).getDataType());
    }

    /**
     * The filter is based on an expression
     */
    @Test
    public void testPostFilter_UseAggregationAliasInFilter() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("cnt", "totalCount", null, "sum(totalCount)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .filterExpression("cnt > 1000")
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           sum("totalCount") AS "cnt"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName", "instance"
                                    HAVING "cnt" > 1000
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("cnt", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());
    }

    /**
     * The filter is based on an expression
     */
    @Test
    public void testPostFilter_UseAggregationFilter() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("totalCount", "totalCount", null, "sum(totalCount)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .filterExpression("totalCount > 1000")
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           sum("totalCount") AS "totalCount"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    GROUP BY "appName", "instance"
                                    HAVING "totalCount" > 1000
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(3, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("totalCount", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());
    }

    @Test
    public void testPostFilter_FilterNotInTheSelectList_H2() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("totalCount", "totalCount", null, "sum(totalCount)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .filterExpression("avgResponseTime > 5")
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        // NOTE that in the WHERE clause, the rhs is 5.0, however, our input is 5
        // This is because the avgResponse is defined as DOUBLE,
        // and there's a type conversion in 'ExpressionTypeValidator'
        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           "sum_totalCount" AS "totalCount",
                                           CASE WHEN ( "sum_totalCount" <> 0 ) THEN ( "sum_responseTime" / "sum_totalCount" ) ELSE ( 0 ) END AS "avgResponseTime"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instance",
                                             sum("totalCount") AS "sum_totalCount",
                                             sum("responseTime") AS "sum_responseTime"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                      GROUP BY "appName", "instance"
                                    )
                                    WHERE "avgResponseTime" > 5.0
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("totalCount", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("avgResponseTime", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void testPostFilter_FilterNotInTheSelectList_CK() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("totalCount", "totalCount", null, "sum(totalCount)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .filterExpression("avgResponseTime > 5")
                                                .groupBy(new LinkedHashSet<>(List.of("appName", "instance")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(clickHouseDialect)
                                                                .build();

        // NOTE that in the WHERE clause, the rhs is 5.0, however, our input is 5
        // This is because the avgResponse is defined as DOUBLE,
        // and there's a type conversion in 'ExpressionTypeValidator'
        Assertions.assertEquals("""
                                    SELECT "appName",
                                           "instance",
                                           "sum_totalCount" AS "totalCount",
                                           "sum_responseTime" / "sum_totalCount" AS "avgResponseTime"
                                    FROM
                                    (
                                      SELECT "appName",
                                             "instance",
                                             sum("totalCount") AS "sum_totalCount",
                                             sum("responseTime") AS "sum_responseTime"
                                      FROM "bithon_http_incoming_metrics"
                                      WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720))
                                      GROUP BY "appName", "instance"
                                    )
                                    WHERE "avgResponseTime" > 5.0
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));

        // Assert the SelectStatement object
        Assertions.assertEquals(4, selectStatement.getSelectorList().size());

        Assertions.assertEquals("appName", selectStatement.getSelectorList().get(0).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(0).getDataType());

        Assertions.assertEquals("instance", selectStatement.getSelectorList().get(1).getOutputName());
        Assertions.assertEquals(IDataType.STRING, selectStatement.getSelectorList().get(1).getDataType());

        Assertions.assertEquals("totalCount", selectStatement.getSelectorList().get(2).getOutputName());
        Assertions.assertEquals(IDataType.LONG, selectStatement.getSelectorList().get(2).getDataType());

        Assertions.assertEquals("avgResponseTime", selectStatement.getSelectorList().get(3).getOutputName());
        Assertions.assertEquals(IDataType.DOUBLE, selectStatement.getSelectorList().get(3).getDataType());
    }

    @Test
    public void testAggregateFunctionColumn_CK() {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(
                                                    new QueryField("t1", "clickedSum", null, "sum(clickedSum)"),
                                                    new QueryField("t2", "clickedCnt", null, "count(clickedCnt)")
                                                ))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(clickHouseDialect)
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
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("t1", "clickedSum", null, "count(clickedSum)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(clickHouseDialect)
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
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("t1", "clickedCnt", null, "sum(clickedCnt)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(clickHouseDialect)
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
        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                         .step(10)
                                                         .window(HumanReadableDuration.parse("1m"))
                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(
                                                    new QueryField("t1", "clickedSum", null, "sum(clickedSum)"),
                                                    new QueryField("t2", "clickedCnt", null, "count(clickedCnt)")
                                                ))
                                                .interval(intervalRequest)
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(clickHouseDialect)
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

        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(currentStart)
                                                         .endISO8601(currentEnd)
                                                         .step(10)

                                                         .window(HumanReadableDuration.parse("1m"))
                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(
                                                    new QueryField("t1", "clickedSum", null, "sum(clickedSum)"),
                                                    new QueryField("t2", "clickedCnt", null, "count(clickedCnt)")
                                                ))
                                                .interval(intervalRequest)
                                                // -1d offset
                                                .offset(HumanReadableDuration.parse("-1d"))
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(clickHouseDialect)
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
        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                         .step(10)
                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("t1", "clickedSum", null, "sum(clickedSum)")))
                                                .interval(intervalRequest)
                                                .filterExpression("appName =~ 'bithon.*' AND instance !~ '192.*'")
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(h2Dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                           "appName",
                                           sum("clickedSum") AS "t1"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND (regexp_like("bithon_http_incoming_metrics"."appName", 'bithon.*', 'nm') AND (NOT regexp_like("bithon_http_incoming_metrics"."instance", '192.*', 'nm')))
                                    GROUP BY "appName", "_timestamp"
                                    """.trim(),
                                selectStatement.toSQL(h2Dialect));
    }

    @Test
    public void test_RegularExpressionMatch_CK() {
        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                         .step(10)
                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("t1", "clickedSum", null, "sum(clickedSum)")))
                                                .interval(intervalRequest)
                                                .filterExpression("appName =~ 'bithon.*' AND instance !~ '192.*'")
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(clickHouseDialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT toUnixTimestamp(toStartOfInterval("timestamp", INTERVAL 10 SECOND)) AS "_timestamp",
                                           "appName",
                                           sumMerge("clickedSum") AS "t1"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720)) AND (match("bithon_http_incoming_metrics"."appName", 'bithon.*') AND (NOT match("bithon_http_incoming_metrics"."instance", '192.*')))
                                    GROUP BY "appName", "_timestamp"
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));
    }

    @Test
    public void test_RegularExpressionMatch_Optimized_CK() {
        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                         .step(10)
                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("t1", "clickedSum", null, "sum(clickedSum)")))
                                                .interval(intervalRequest)
                                                .filterExpression("appName =~ '^bithon.*' AND instance !~ '^192.*'")
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(clickHouseDialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT toUnixTimestamp(toStartOfInterval("timestamp", INTERVAL 10 SECOND)) AS "_timestamp",
                                           "appName",
                                           sumMerge("clickedSum") AS "t1"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= fromUnixTimestamp(1722000120)) AND ("timestamp" < fromUnixTimestamp(1722000720)) AND (startsWith("bithon_http_incoming_metrics"."appName", 'bithon') AND (NOT startsWith("bithon_http_incoming_metrics"."instance", '192')))
                                    GROUP BY "appName", "_timestamp"
                                    """.trim(),
                                selectStatement.toSQL(clickHouseDialect));
    }

    @Test
    public void test_RegularExpressionMatch_MySQL() {
        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                         .step(10)
                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("t1", "clickedSum", null, "sum(clickedSum)")))
                                                .interval(intervalRequest)
                                                .filterExpression("appName =~ 'bithon.*' AND instance !~ '192.*'")
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(mysql)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT UNIX_TIMESTAMP(`timestamp`) div 10 * 10 AS `_timestamp`,
                                           `appName`,
                                           sum(`clickedSum`) AS `t1`
                                    FROM `bithon_http_incoming_metrics`
                                    WHERE (`timestamp` >= '2024-07-26T21:22:00.000+08:00') AND (`timestamp` < '2024-07-26T21:32:00.000+08:00') AND (REGEXP_LIKE(`bithon_http_incoming_metrics`.`appName`, 'bithon.*') AND (NOT REGEXP_LIKE(`bithon_http_incoming_metrics`.`instance`, '192.*')))
                                    GROUP BY `appName`, `_timestamp`
                                    """.trim(),
                                selectStatement.toSQL(mysql));
    }

    @Test
    public void test_RegularExpressionMatch_PG() {
        ISqlDialect pg = new PostgreSqlDialect();
        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                         .step(10)
                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("t1", "clickedSum", null, "sum(clickedSum)")))
                                                .interval(intervalRequest)
                                                .filterExpression("appName =~ 'bithon.*' AND instance !~ '192.*'")
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(pg)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT  FLOOR(EXTRACT(EPOCH FROM "timestamp" AT TIME ZONE 'UTC-8') / 10) * 10 AS "_timestamp",
                                           "appName",
                                           sum("clickedSum") AS "t1"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND (("bithon_http_incoming_metrics"."appName" ~ 'bithon.*') AND (NOT ("bithon_http_incoming_metrics"."instance" ~ '192.*')))
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
        IntervalRequest intervalRequest = IntervalRequest.builder()
                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                         .step(10)
                                                         .build();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(
                                                    new QueryField("maxResponseTime", "responseTime", null, "max(responseTime)"),
                                                    new QueryField("minResponseTime", "responseTime", null, "min(responseTime)"),
                                                    new QueryField("avgResponseTime", "responseTime", null, "sum(responseTime)/count()"),
                                                    new QueryField("count", "1", null, "count()")
                                                ))
                                                .interval(intervalRequest)
                                                .filterExpression("appName =~ 'bithon.*' AND instance !~ '192.*'")
                                                .groupBy(new LinkedHashSet<>(List.of("appName")))
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, intervalRequest.calculateStep()))
                                                                .sqlDialect(pg)
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
                                      WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND (("bithon_http_incoming_metrics"."appName" ~ 'bithon.*') AND (NOT ("bithon_http_incoming_metrics"."instance" ~ '192.*')))
                                      GROUP BY "appName", "_timestamp"
                                    )
                                    """.trim(),
                                selectStatement.toSQL(pg));
    }

    @Test
    public void test_SelectStatement() {
        ISqlDialect dialect = new H2SqlDialect();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(
                                                    new QueryField("maxResponseTime", "responseTime", null, null),
                                                    new QueryField("minResponseTime", "responseTime", null, null)
                                                ))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .filterExpression("appName = 'bithon'")
                                                .limit(Limit.fromLong(100))
                                                .orderBy(OrderBy.builder()
                                                                .name("maxResponseTime")
                                                                .order(Order.asc)
                                                                .build())
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(dialect)
                                                                .buildSelectStatement();

        Assertions.assertEquals("""
                                    SELECT "responseTime" AS "maxResponseTime",
                                           "responseTime" AS "minResponseTime"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("bithon_http_incoming_metrics"."appName" = 'bithon')
                                    ORDER BY "maxResponseTime" asc
                                    LIMIT 100
                                    """.trim(),
                                selectStatement.toSQL(dialect));
    }

    @Test
    public void test_AggregateWithoutGroupBy() {
        ISqlDialect dialect = new H2SqlDialect();
        QueryRequest queryRequest = QueryRequest.builder()
                                                .fields(List.of(new QueryField("maxResponseTime", "responseTime", null, "max(responseTime)")))
                                                .interval(IntervalRequest.builder()
                                                                         .startISO8601(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"))
                                                                         .endISO8601(TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"))
                                                                         .build())
                                                .filterExpression("appName = 'bithon'")
                                                .build();
        SelectStatement selectStatement = SelectStatementBuilder.from(QueryConverter.toQuery(schema, queryRequest, null))
                                                                .sqlDialect(dialect)
                                                                .build();

        Assertions.assertEquals("""
                                    SELECT max("responseTime") AS "maxResponseTime"
                                    FROM "bithon_http_incoming_metrics"
                                    WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("bithon_http_incoming_metrics"."appName" = 'bithon')
                                    """.trim(),
                                selectStatement.toSQL(dialect));
    }
}
