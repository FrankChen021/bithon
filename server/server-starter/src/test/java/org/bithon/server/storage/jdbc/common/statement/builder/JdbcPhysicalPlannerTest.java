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
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.plan.logical.ILogicalPlan;
import org.bithon.server.datasource.query.plan.logical.LogicalAggregate;
import org.bithon.server.datasource.query.plan.logical.LogicalTableScan;
import org.bithon.server.datasource.query.plan.physical.IPhysicalPlan;
import org.bithon.server.datasource.reader.clickhouse.AggregateFunctionColumn;
import org.bithon.server.datasource.reader.h2.H2SqlDialect;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.JdbcPhysicalPlanner;
import org.bithon.server.datasource.store.IDataStoreSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

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
                                                             return null;
                                                         }
                                                     },
                                                     null,
                                                     null);

    private ISqlDialect h2Dialect = new H2SqlDialect();

    @Test
    public void testTableScanPlan() {
        ILogicalPlan logicalPlan = new LogicalTableScan("test_table",
                                                        List.of(new Selector("a1", "a1", IDataType.DOUBLE)),
                                                        null);
        IPhysicalPlan physicalPlan = new JdbcPhysicalPlanner(null, this.h2Dialect, this.schema)
            .plan(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                              TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")),
                  logicalPlan);
        Assertions.assertEquals("""
                                    JdbcReadStep
                                        SELECT "a1" AS "a1"
                                        FROM "test_table" AS "test_table"
                                    """, physicalPlan.serializeToText());
    }

    @Test
    public void testTableScanPlanWithFilter() {
        ILogicalPlan logicalPlan = new LogicalTableScan("test_table",
                                                        List.of(new Selector("a1", "a1", IDataType.DOUBLE)),
                                                        new LogicalExpression.AND(new ComparisonExpression.EQ(IdentifierExpression.of("a1"), LiteralExpression.ofString("jacky"))));
        IPhysicalPlan physicalPlan = new JdbcPhysicalPlanner(null, this.h2Dialect, this.schema)
            .plan(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                              TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")),
                  logicalPlan);
        Assertions.assertEquals("""
                                    JdbcReadStep
                                        SELECT "a1" AS "a1"
                                        FROM "test_table" AS "test_table"
                                        WHERE ("a1" = 'jacky')
                                    """, physicalPlan.serializeToText());
    }

    @Test
    public void testAggregatePlan() {
        ILogicalPlan tableScan = new LogicalTableScan("bithon-jvm-metrics",
                                                      List.of(new Selector("totalCount", "totalCount", IDataType.DOUBLE)),
                                                      new LogicalExpression.AND(new ComparisonExpression.EQ(IdentifierExpression.of("a1"), LiteralExpression.ofString("jacky"))));

        ILogicalPlan aggregatePlan = new LogicalAggregate("sum",
                                                          tableScan,
                                                          new AggregateLongSumColumn("totalCount", "totalCount"),
                                                          List.of("appName"),
                                                          null);

        IPhysicalPlan physicalPlan = new JdbcPhysicalPlanner(null, h2Dialect, this.schema)
            .plan(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"),
                              TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")),
                  aggregatePlan);
        Assertions.assertEquals("""
                                    JdbcReadStep
                                        SELECT "appName",
                                               sum("totalCount") AS "totalCount"
                                        FROM "bithon_jvm_metrics"
                                        WHERE ("timestamp" >= '2024-07-26T21:22:00.000+08:00') AND ("timestamp" < '2024-07-26T21:32:00.000+08:00') AND ("bithon_jvm_metrics"."a1" = 'jacky')
                                        GROUP BY "appName"
                                    """, physicalPlan.serializeToText());
    }
}
