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
import org.bithon.server.storage.common.expression.InvalidExpressionException;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * @author Frank Chen
 * @date 19/9/23 9:26 pm
 */
public class FilterExpressionToFiltersTest {

    @Test
    public void testValidatedFilterExpression() {
        DataSourceSchema schema = new DataSourceSchema(
            "schema",
            "schema",
            new TimestampSpec("timestamp", null, null),
            Collections.singletonList(new StringColumn("a", "a")),
            Collections.emptyList()
        );

        // Unary literal is not a valid expression
        Assert.assertThrows(InvalidExpressionException.class, () -> FilterExpressionToFilters.toExpression(schema, "123", null));
        Assert.assertThrows(InvalidExpressionException.class, () -> FilterExpressionToFilters.toExpression(schema, "'123'", null));
        Assert.assertThrows(InvalidExpressionException.class, () -> FilterExpressionToFilters.toExpression(schema, "123.123", null));

        Assert.assertThrows(InvalidExpressionException.class, () -> FilterExpressionToFilters.toExpression(schema, "a", null));


        // Unary function expression is not a valid filter
        Assert.assertThrows(InvalidExpressionException.class, () -> FilterExpressionToFilters.toExpression(schema, "trim(a)", null));

        Assert.assertTrue(FilterExpressionToFilters.toExpression(schema, "a > 'a'", null) instanceof ComparisonExpression);
        Assert.assertTrue(FilterExpressionToFilters.toExpression(schema, "a > 'a' AND a < 'a'", null) instanceof LogicalExpression);

        Assert.assertTrue(FilterExpressionToFilters.toExpression(schema, "startsWith(a, 'a')", null) instanceof FunctionExpression);
        Assert.assertTrue(FilterExpressionToFilters.toExpression(schema, "endsWith(a, 'a')", null) instanceof FunctionExpression);
        Assert.assertTrue(FilterExpressionToFilters.toExpression(schema, "hasToken(a, 'a')", null) instanceof FunctionExpression);
    }
}
