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
import org.bithon.component.commons.expression.validation.ExpressionValidationException;
import org.bithon.server.storage.common.expression.InvalidExpressionException;
import org.bithon.server.storage.datasource.DefaultSchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.LongColumn;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.web.service.datasource.api.impl.QueryFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author Frank Chen
 * @date 19/9/23 9:26 pm
 */
public class QueryFilterTest {

    private final DefaultSchema schema = new DefaultSchema(
        "schema",
        "schema",
        new TimestampSpec("timestamp"),
        Arrays.asList(new StringColumn("a", "a"),
                      new LongColumn("intB", "intB")),
        Collections.emptyList()
    );

    @Test
    public void testValidatedFilterExpression() {
        // Unary literal is not a valid expression
        Assert.assertThrows(InvalidExpressionException.class, () -> QueryFilter.build(schema, "123"));
        Assert.assertThrows(InvalidExpressionException.class, () -> QueryFilter.build(schema, "'123'"));
        Assert.assertThrows(InvalidExpressionException.class, () -> QueryFilter.build(schema, "123.123"));

        Assert.assertThrows(InvalidExpressionException.class, () -> QueryFilter.build(schema, "a"));

        Assert.assertThrows(ExpressionValidationException.class, () -> QueryFilter.build(schema, "a in ('5', 6)"));
        QueryFilter.build(schema, "a in ('5', '6')");

        // Unary function expression is not a valid filter
        Assert.assertThrows(InvalidExpressionException.class, () -> QueryFilter.build(schema, "trim(a)"));

        Assert.assertTrue(QueryFilter.build(schema, "a > 'a'") instanceof ComparisonExpression);
        Assert.assertTrue(QueryFilter.build(schema, "a > 'a' AND a < 'a'") instanceof LogicalExpression);

        Assert.assertTrue(QueryFilter.build(schema, "startsWith(a, 'a')") instanceof FunctionExpression);
        Assert.assertTrue(QueryFilter.build(schema, "endsWith(a, 'a')") instanceof FunctionExpression);
        Assert.assertTrue(QueryFilter.build(schema, "hasToken(a, 'a')") instanceof FunctionExpression);
    }
}
