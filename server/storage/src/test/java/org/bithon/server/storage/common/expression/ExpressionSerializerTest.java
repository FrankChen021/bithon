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

package org.bithon.server.storage.common.expression;

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.MapAccessExpression;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Frank Chen
 * @date 28/8/23 10:39 am
 */
public class ExpressionSerializerTest {

    @Test
    public void testQuotedIdentifier() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1");

        Assert.assertEquals("\"a\" = 1", expr.serializeToText());
    }

    @Test
    public void testUnQuotedIdentifier() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1");

        Assert.assertEquals("a = 1", expr.serializeToText(null));
    }

    @Test
    public void testQualifiedIdentifier() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1");

        Assert.assertEquals("default.a = 1", new ExpressionSerializer("default", null).serialize(expr));
        Assert.assertEquals("\"default\".\"a\" = 1", new ExpressionSerializer("default", (s) -> "\"" + s + "\"").serialize(expr));
    }

    @Test
    public void testMapAccessExpression() {
        IExpression expr = ExpressionASTBuilder.builder().build("a ['b']");
        Assert.assertEquals("a['b']", expr.serializeToText(null));

        expr = ExpressionASTBuilder.builder().build("i > 5 AND colors['today'] = 'red'");
        Assert.assertTrue(expr instanceof LogicalExpression.AND);
        IExpression right = ((LogicalExpression.AND) expr).getOperands().get(1);
        Assert.assertTrue(right instanceof ComparisonExpression.EQ);
        Assert.assertTrue(((ComparisonExpression.EQ) right).getLeft() instanceof MapAccessExpression);
    }
}
