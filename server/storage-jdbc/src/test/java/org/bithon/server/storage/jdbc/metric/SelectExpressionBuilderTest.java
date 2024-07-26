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

import java.util.Arrays;
import java.util.Collections;

/**
 * @author frank.chen021@outlook.com
 * @date 26/7/24 10:44 am
 */
public class SelectExpressionBuilderTest {

    private final ISchema schema = new DefaultSchema("test-metrics",
                                                     "test-metrics",
                                                     new TimestampSpec("timestamp"),
                                                     Arrays.asList(new StringColumn("appName", "appName"), new StringColumn("instance", "instance")),
                                                     Arrays.asList(new AggregateLongSumColumn("responseTime", "responseTime"),
                                                                   new AggregateLongSumColumn("totalCount", "totalCount"),
                                                                   new AggregateLongLastColumn("activeThreadCount", "activeThreadCount"),
                                                                   new AggregateLongLastColumn("totalThreadCount", "totalThreadCount")
                                                     ),
                                                     null,
                                                     new IDataStoreSpec() {
                                                         @Override
                                                         public String getStore() {
                                                             return "test_metrics";
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
        public String timeFloorExpression(IExpression timestampExpression, long interval) {
            return "";
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
        public String firstAggregator(String field, String name, long window) {
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
        public String timeFloorExpression(IExpression timestampExpression, long interval) {
            return "";
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
        public String firstAggregator(String field, String name, long window) {
            return StringUtils.format(
                "FIRST_VALUE(\"%s\") OVER (partition by %s ORDER BY \"timestamp\") AS \"%s\"",
                field,
                this.timeFloorExpression(new IdentifierExpression("timestamp"), window),
                name);
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
    public void testSimpleAggregation() {
        QueryExpression queryExpression = SelectExpressionBuilder.builder()
                                                                 .sqlDialect(dialect)
                                                                 .fields(Collections.singletonList(new SelectColumn(new Expression("sum(totalCount)"), new ColumnAlias("totalCount"))))
                                                                 .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                 .dataSource(schema)
                                                                 .buildPipeline();

        SqlGenerator sqlGenerator = new SqlGenerator(dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT sum(totalCount) AS totalCount
                            FROM test_metrics
                            WHERE timestamp >= '2024-07-26T21:22:00.000+08:00' AND timestamp < '2024-07-26T21:32:00.000+08:00'
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testPostCalculationExpression() {
        QueryExpression queryExpression = SelectExpressionBuilder.builder()
                                                                 .sqlDialect(dialect)
                                                                 .fields(Collections.singletonList(new SelectColumn(new Expression("sum(responseTime)/sum(totalCount)"), new ColumnAlias("avg"))))
                                                                 .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                 .dataSource(schema)
                                                                 .buildPipeline();

        SqlGenerator sqlGenerator = new SqlGenerator(dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT responseTime / totalCount AS avg
                            FROM
                            (
                              SELECT sum(responseTime) AS responseTime,
                              sum(totalCount) AS totalCount
                              FROM test_metrics
                              WHERE timestamp >= '2024-07-26T21:22:00.000+08:00' AND timestamp < '2024-07-26T21:32:00.000+08:00'
                            ) AS tbl1
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testWindowFunction() {
        QueryExpression queryExpression = SelectExpressionBuilder.builder()
                                                                 .sqlDialect(h2Dialect)
                                                                 .fields(Collections.singletonList(new SelectColumn(new Expression("first(activeThreadCount)"), new ColumnAlias("activeThreadCount"))))
                                                                 .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                 .dataSource(schema)
                                                                 .buildPipeline();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT FIRST_VALUE("activeThreadCount") OVER (partition by  ORDER BY "timestamp") AS "activeThreadCount"
                            FROM "test_metrics"
                            WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                            """.trim(),
                            sqlGenerator.getSQL());
    }

    @Test
    public void testWindowFunctionAndAggregator() {
        QueryExpression queryExpression = SelectExpressionBuilder.builder()
                                                                 .sqlDialect(h2Dialect)
                                                                 .fields(Collections.singletonList(new SelectColumn(new Expression("first(activeThreadCount)/sum(totalThreadCount)"), new ColumnAlias("ratio"))))
                                                                 .interval(Interval.of(TimeSpan.fromISO8601("2024-07-26T21:22:00.000+0800"), TimeSpan.fromISO8601("2024-07-26T21:32:00.000+0800")))
                                                                 .orderBy(OrderBy.builder().name("timestamp").order(Order.asc).build())
                                                                 .dataSource(schema)
                                                                 .buildPipeline();

        SqlGenerator sqlGenerator = new SqlGenerator(h2Dialect);
        queryExpression.accept(sqlGenerator);

        Assert.assertEquals("""
                            SELECT "activeThreadCount" / "totalThreadCount" AS "ratio"
                            FROM
                            (
                              SELECT "activeThreadCount",
                              sum("totalThreadCount") AS "totalThreadCount"
                              FROM
                              (
                                SELECT FIRST_VALUE("activeThreadCount") OVER (partition by  ORDER BY "timestamp") AS "activeThreadCount",
                                "totalThreadCount"
                                FROM "test_metrics"
                                WHERE "timestamp" >= '2024-07-26T21:22:00.000+08:00' AND "timestamp" < '2024-07-26T21:32:00.000+08:00'
                                ORDER BY "timestamp" asc
                              ) AS "tbl1"
                            ) AS "tbl2"
                            """.trim(),
                            sqlGenerator.getSQL());
    }
}
