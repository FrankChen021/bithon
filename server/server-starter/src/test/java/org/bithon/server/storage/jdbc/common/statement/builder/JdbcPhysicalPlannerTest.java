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
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.DefaultSchema;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.column.ExpressionColumn;
import org.bithon.server.datasource.column.StringColumn;
import org.bithon.server.datasource.column.aggregatable.last.AggregateLongLastColumn;
import org.bithon.server.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.plan.logical.ILogicalPlan;
import org.bithon.server.datasource.query.plan.physical.IPhysicalPlan;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.bithon.server.datasource.reader.clickhouse.AggregateFunctionColumn;
import org.bithon.server.datasource.reader.h2.H2SqlDialect;
import org.bithon.server.datasource.reader.jdbc.JdbcDataSourceReader;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.JdbcPhysicalPlanner;
import org.bithon.server.datasource.store.IDataStoreSpec;
import org.bithon.server.metric.expression.ast.MetricExpressionASTBuilder;
import org.bithon.server.metric.expression.plan.LogicalPlanBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/6/5 20:58
 */
public class JdbcPhysicalPlannerTest {
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
                                                             return new JdbcDataSourceReader(null, new H2SqlDialect(), QuerySettings.DEFAULT);
                                                         }
                                                     },
                                                     null,
                                                     null);

    private final ISqlDialect h2Dialect = new H2SqlDialect();

    @Test
    public void testTableScanPlan() {
        IExpression ast = MetricExpressionASTBuilder.parse("bithon-jvm-metrics.totalCount");
        ILogicalPlan logicalPlan = ast.accept(new LogicalPlanBuilder(this.schema));

        IPhysicalPlan physicalPlan = new JdbcPhysicalPlanner(null, this.h2Dialect, this.schema)
            .plan(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                              TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")),
                  logicalPlan);

        Assertions.assertEquals("""
                                    JdbcReadStep
                                        SELECT "totalCount" AS "totalCount"
                                        FROM "bithon-jvm-metrics" AS "bithon-jvm-metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00')
                                    """,
                                physicalPlan.serializeToText());
    }

    @Test
    public void testTableScanPlanWithFilter() {
        IExpression ast = MetricExpressionASTBuilder.parse("bithon-jvm-metrics.totalCount{appName=\"jacky\"}");
        ILogicalPlan logicalPlan = ast.accept(new LogicalPlanBuilder(this.schema));
        IPhysicalPlan physicalPlan = new JdbcPhysicalPlanner(null, this.h2Dialect, this.schema)
            .plan(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                              TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")),
                  logicalPlan);

        Assertions.assertEquals("""
                                    JdbcReadStep
                                        SELECT "totalCount" AS "totalCount"
                                        FROM "bithon-jvm-metrics" AS "bithon-jvm-metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("appName" = 'jacky')
                                    """,
                                physicalPlan.serializeToText());
    }

    @Test
    public void test_SumAggregate() {
        IExpression ast = MetricExpressionASTBuilder.parse("sum(bithon-jvm-metrics.totalCount{appName=\"jacky\"})");
        ILogicalPlan logicalPlan = ast.accept(new LogicalPlanBuilder(this.schema));
        IPhysicalPlan physicalPlan = new JdbcPhysicalPlanner(null, this.h2Dialect, this.schema)
            .plan(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                              TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")),
                  logicalPlan);

        // TODO: Remove duplicate timestamp filter
        Assertions.assertEquals("""
                                    JdbcReadStep
                                        SELECT sum("totalCount") AS "totalCount"
                                        FROM "bithon_jvm_metrics"
                                        WHERE ("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND (("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("bithon_jvm_metrics"."appName" = 'jacky'))
                                    """,
                                physicalPlan.serializeToText());
    }

    @Test
    public void test_SumAggregate_GroupBy() {
        IExpression ast = MetricExpressionASTBuilder.parse("sum(bithon-jvm-metrics.totalCount{appName=\"jacky\"}) by (appName)");
        ILogicalPlan logicalPlan = ast.accept(new LogicalPlanBuilder(this.schema));
        IPhysicalPlan physicalPlan = new JdbcPhysicalPlanner(null, this.h2Dialect, this.schema)
            .plan(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                              TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")),
                  logicalPlan);

        // TODO: Remove duplicate timestamp filter
        Assertions.assertEquals("""
                                    JdbcReadStep
                                        SELECT "appName",
                                               sum("totalCount") AS "totalCount"
                                        FROM "bithon_jvm_metrics"
                                        WHERE ("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND (("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("bithon_jvm_metrics"."appName" = 'jacky'))
                                        GROUP BY "appName"
                                    """,
                                physicalPlan.serializeToText());
    }

    @Test
    public void test_SumAggregate_GroupBy_TimeSeries() {
        IExpression ast = MetricExpressionASTBuilder.parse("sum(bithon-jvm-metrics.totalCount{appName=\"jacky\"}) by (appName)");
        ILogicalPlan logicalPlan = ast.accept(new LogicalPlanBuilder(this.schema));
        IPhysicalPlan physicalPlan = new JdbcPhysicalPlanner(null, this.h2Dialect, this.schema)
            .plan(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                              TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800"),
                              Duration.ofSeconds(10),
                              new IdentifierExpression("timestamp")),
                  logicalPlan);

        // TODO: Remove duplicate timestamp filter
        Assertions.assertEquals("""
                                    JdbcReadStep
                                        SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
                                               "appName",
                                               sum("totalCount") AS "totalCount"
                                        FROM "bithon_jvm_metrics"
                                        WHERE ("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND (("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("bithon_jvm_metrics"."appName" = 'jacky'))
                                        GROUP BY "appName", "_timestamp"
                                    """,
                                physicalPlan.serializeToText());
    }

    @Test
    public void test_LastAggregate() {
        IExpression ast = MetricExpressionASTBuilder.parse("last(bithon-jvm-metrics.totalCount{appName=\"jacky\"}) by (appName)");
        ILogicalPlan logicalPlan = ast.accept(new LogicalPlanBuilder(this.schema));
        IPhysicalPlan physicalPlan = new JdbcPhysicalPlanner(null, this.h2Dialect, this.schema)
            .plan(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                              TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")),
                  logicalPlan);

        // TODO: Remove duplicate timestamp filter
        Assertions.assertEquals("""
                                    JdbcReadStep
                                        SELECT "appName",
                                               "totalCount"
                                        FROM
                                        (
                                          SELECT "appName",
                                                 FIRST_VALUE("totalCount") OVER (PARTITION BY (UNIX_TIMESTAMP("timestamp") / 600) * 600 ORDER BY "timestamp" ASC) AS "totalCount"
                                          FROM "bithon_jvm_metrics"
                                          WHERE ("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND (("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("bithon_jvm_metrics"."appName" = 'jacky'))
                                        )
                                        GROUP BY "appName", "totalCount"
                                    """,
                                physicalPlan.serializeToText());
    }

    @Test
    public void test_Arithmetic_TwoExpressions() {
        IExpression ast = MetricExpressionASTBuilder.parse("sum(bithon-jvm-metrics.count4xx{appName=\"jacky\"}) + sum(bithon-jvm-metrics.count5xx{appName=\"jacky\"})");
        ILogicalPlan logicalPlan = ast.accept(new LogicalPlanBuilder(this.schema));
        IPhysicalPlan physicalPlan = new JdbcPhysicalPlanner(null, this.h2Dialect, this.schema)
            .plan(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                              TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")),
                  logicalPlan);

        // TODO: Remove duplicate timestamp filter
        Assertions.assertEquals("""
                                    AddStep, Result Column: value, Retained Columns: []
                                        lhs:\s
                                            JdbcReadStep
                                                SELECT sum("count4xx") AS "count4xx"
                                                FROM "bithon_jvm_metrics"
                                                WHERE ("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND (("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("bithon_jvm_metrics"."appName" = 'jacky'))
                                        rhs:\s
                                            JdbcReadStep
                                                SELECT sum("count5xx") AS "count5xx"
                                                FROM "bithon_jvm_metrics"
                                                WHERE ("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND (("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("bithon_jvm_metrics"."appName" = 'jacky'))
                                    """,
                                physicalPlan.serializeToText());
    }

    @Test
    public void test_Arithmetic_WithLiteral() {
        IExpression ast = MetricExpressionASTBuilder.parse("sum(bithon-jvm-metrics.count4xx{appName=\"jacky\"}) + 5");
        ILogicalPlan logicalPlan = ast.accept(new LogicalPlanBuilder(this.schema));
        IPhysicalPlan physicalPlan = new JdbcPhysicalPlanner(null, this.h2Dialect, this.schema)
            .plan(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                              TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")),
                  logicalPlan);

        // TODO: Remove duplicate timestamp filter
        Assertions.assertEquals("""
                                    AddStep, Result Column: value, Retained Columns: []
                                        lhs:\s
                                            JdbcReadStep
                                                SELECT sum("count4xx") AS "count4xx"
                                                FROM "bithon_jvm_metrics"
                                                WHERE ("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND (("bithon_jvm_metrics"."timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("bithon_jvm_metrics"."timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("bithon_jvm_metrics"."appName" = 'jacky'))
                                        rhs:\s
                                            5
                                    """,
                                physicalPlan.serializeToText());
    }
}
