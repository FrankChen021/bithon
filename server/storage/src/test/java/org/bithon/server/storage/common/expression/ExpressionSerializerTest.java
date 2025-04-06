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

import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.MapAccessExpression;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Frank Chen
 * @date 28/8/23 10:39 am
 */
public class ExpressionSerializerTest {

    @Test
    public void testQuotedIdentifier() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1");

        Assertions.assertEquals("\"a\" = 1", expr.serializeToText());
    }

    @Test
    public void testUnQuotedIdentifier() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1");

        Assertions.assertEquals("a = 1", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testQualifiedIdentifier() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1");

        Assertions.assertEquals("default.a = 1", new ExpressionSerializer("default", null).serialize(expr));
        Assertions.assertEquals("\"default\".\"a\" = 1", new ExpressionSerializer("default", IdentifierQuotaStrategy.DOUBLE_QUOTE).serialize(expr));
    }

    @Test
    public void testMapAccessExpression() {
        IExpression expr = ExpressionASTBuilder.builder().build("a ['b']");
        Assertions.assertEquals("a['b']", expr.serializeToText(IdentifierQuotaStrategy.NONE));

        expr = ExpressionASTBuilder.builder().build("i > 5 AND colors['today'] = 'red'");
        Assertions.assertInstanceOf(LogicalExpression.AND.class, expr);
        IExpression right = ((LogicalExpression.AND) expr).getOperands().get(1);
        Assertions.assertInstanceOf(ComparisonExpression.EQ.class, right);
        Assertions.assertInstanceOf(MapAccessExpression.class, ((ComparisonExpression.EQ) right).getLhs());

        expr = ExpressionASTBuilder.builder().build("5 * colors['today']");
        Assertions.assertInstanceOf(ArithmeticExpression.class, expr);

        // The constant expression optimizer will convert the expression to colors['today'] * 5
        // Maybe in such as case where OPTIMIZATION does not apply, we should not optimize the expression
        Assertions.assertInstanceOf(MapAccessExpression.class, ((ArithmeticExpression) expr).getLhs());
    }
}
