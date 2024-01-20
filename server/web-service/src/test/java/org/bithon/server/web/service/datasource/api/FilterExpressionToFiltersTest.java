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

package org.bithon.server.web.service.datasource.api;

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.common.expression.InvalidExpressionException;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.LongColumn;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author Frank Chen
 * @date 19/9/23 9:26 pm
 */
public class FilterExpressionToFiltersTest {

    private final DataSourceSchema schema = new DataSourceSchema(
        "schema",
        "schema",
        new TimestampSpec("timestamp", null, null),
        Arrays.asList(new StringColumn("a", "a"), new LongColumn("intB", "intB")),
        Collections.emptyList()
    );

    @Test
    public void testValidatedFilterExpression() {
        FilterExpressionToFilters.toExpression(schema, "tags.clickhouse.cluster = 'cluster' AND tags.clickhouse.user = 'lv' AND tags.clickhouse.queryType = 'INSERT' AND a = 'SERVER' ", null);

        // Unary literal is not a valid expression
        Assert.assertThrows(InvalidExpressionException.class, () -> FilterExpressionToFilters.toExpression(schema, "123", null));
        Assert.assertThrows(InvalidExpressionException.class, () -> FilterExpressionToFilters.toExpression(schema, "'123'", null));
        Assert.assertThrows(InvalidExpressionException.class, () -> FilterExpressionToFilters.toExpression(schema, "123.123", null));

        Assert.assertThrows(InvalidExpressionException.class, () -> FilterExpressionToFilters.toExpression(schema, "a", null));

        Assert.assertThrows(InvalidExpressionException.class, () -> FilterExpressionToFilters.toExpression(schema, "a in ('5', 6)", null));
        FilterExpressionToFilters.toExpression(schema, "a in ('5', '6')", null);

        // Unary function expression is not a valid filter
        Assert.assertThrows(InvalidExpressionException.class, () -> FilterExpressionToFilters.toExpression(schema, "trim(a)", null));

        Assert.assertTrue(FilterExpressionToFilters.toExpression(schema, "a > 'a'", null) instanceof ComparisonExpression);
        Assert.assertTrue(FilterExpressionToFilters.toExpression(schema, "a > 'a' AND a < 'a'", null) instanceof LogicalExpression);

        Assert.assertTrue(FilterExpressionToFilters.toExpression(schema, "startsWith(a, 'a')", null) instanceof FunctionExpression);
        Assert.assertTrue(FilterExpressionToFilters.toExpression(schema, "endsWith(a, 'a')", null) instanceof FunctionExpression);
        Assert.assertTrue(FilterExpressionToFilters.toExpression(schema, "hasToken(a, 'a')", null) instanceof FunctionExpression);
    }

    @Test
    public void test_TypeImplicitConversion() {
        // Long ---> String
        Assert.assertEquals("a > '5'",
                            FilterExpressionToFilters.toExpression(schema, "5 < a", null)
                                                     .serializeToText(null));
        Assert.assertEquals("a > '5'", FilterExpressionToFilters.toExpression(schema, "a > 5", null).serializeToText(null));

        // String ---> Int
        Assert.assertEquals("intB > 5", FilterExpressionToFilters.toExpression(schema, "intB > '5'", null).serializeToText(null));
        Assert.assertThrows(InvalidExpressionException.class, () -> FilterExpressionToFilters.toExpression(schema,
                                                                                                           "intB > 'invalid'",
                                                                                                           null));


        // String ---> DateTime
        Assert.assertEquals("timestamp > '2023-01-04T00:00:00.000+08:00'",
                            FilterExpressionToFilters.toExpression(schema,
                                                                   "timestamp > '2023-01-04 00:00:00'", null)
                                                     .serializeToText(null));
        Assert.assertThrows(InvalidExpressionException.class, () -> FilterExpressionToFilters.toExpression(schema,
                                                                                                           "timestamp > 'invalid'",
                                                                                                           null));

        // Long ---> DateTime
        TimeSpan timeSpan = TimeSpan.fromISO8601("2023-01-04T00:00:00.000+08:00");
        Assert.assertEquals("timestamp > '2023-01-04T00:00:00.000+08:00'",
                            FilterExpressionToFilters.toExpression(schema,
                                                                   "timestamp > " + timeSpan.getMilliseconds(), null)
                                                     .serializeToText(null));
    }

    @Test
    public void test_UncompleteLogicalExpression() {
        Assert.assertThrows(InvalidExpressionException.class,
                            () ->
                                FilterExpressionToFilters.toExpression(schema, "a = 'INFO' and a", null)
                           );

        FilterExpressionToFilters.toExpression(schema, "a = 'INFO' and startsWith(a, 'a')", null);
    }

    @Test
    public void test_IdentifierInFunction() {
        FilterExpressionToFilters.toExpression(schema, "hasToken(a, 'a')", null);

        Assert.assertThrows(InvalidExpressionException.class,
                            () ->
                                FilterExpressionToFilters.toExpression(schema, "hasToken(no_exist_column, 'a')", null));
    }
}
