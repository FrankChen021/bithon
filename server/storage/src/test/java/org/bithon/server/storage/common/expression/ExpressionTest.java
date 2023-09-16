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
import org.bithon.component.commons.expression.MacroExpression;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/8/19 17:23
 */
public class ExpressionTest {

    @Test
    public void testIn() {
        IExpression expr = ExpressionASTBuilder.build("5 in (1,2)", null);
        Assert.assertFalse((Boolean) expr.evaluate(null));
    }

    @Test
    public void testIn_2() {
        IExpression expr = ExpressionASTBuilder.build("5 in (5)", null);
        Assert.assertTrue((Boolean) expr.evaluate(null));
    }

    @Test
    public void testIn_3() {
        IExpression expr = ExpressionASTBuilder.build("5 in (5,6)", null);
        Assert.assertTrue((Boolean) expr.evaluate(null));
    }

    @Test
    public void testLogical_ConsecutiveAND() {
        IExpression expr = ExpressionASTBuilder.build("a = 1 AND b = 1 AND c = 1 AND d = 1", null);
        Assert.assertTrue(expr instanceof LogicalExpression.AND);
        Assert.assertEquals(4, ((LogicalExpression.AND) expr).getOperands().size());
        for (int i = 0; i < 4; i++) {
            Assert.assertTrue(((LogicalExpression.AND) expr).getOperands().get(i) instanceof ComparisonExpression);
        }
    }

    @Test
    public void testLogical_OR() {
        IExpression expr = ExpressionASTBuilder.build("a = 1 OR b = 1", null);
        Assert.assertTrue(expr instanceof LogicalExpression.OR);
        Assert.assertEquals("OR", ((LogicalExpression.OR) expr).getOperator());
    }

    @Test
    public void testLogical_ConsecutiveOR() {
        IExpression expr = ExpressionASTBuilder.build("a = 1 OR b = 1 OR c = 1 OR d = 1", null);
        Assert.assertTrue(expr instanceof LogicalExpression.OR);
        Assert.assertEquals(4, ((LogicalExpression.OR) expr).getOperands().size());
        for (int i = 0; i < 4; i++) {
            Assert.assertTrue(((LogicalExpression.OR) expr).getOperands().get(i) instanceof ComparisonExpression);
        }
    }

    @Test
    public void testLogical_AND_OR() {
        IExpression expr = ExpressionASTBuilder.build("a = 1 AND b = 1 AND c = 1 OR d = 1", null);
        Assert.assertTrue(expr instanceof LogicalExpression.OR);
        Assert.assertEquals(2, ((LogicalExpression.OR) expr).getOperands().size());

        Assert.assertTrue(((LogicalExpression.OR) expr).getOperands().get(0) instanceof LogicalExpression.AND);
        Assert.assertTrue(((LogicalExpression.OR) expr).getOperands().get(1) instanceof ComparisonExpression);
    }

    @Test
    public void testMacroExpression() {
        IExpression expr = ExpressionASTBuilder.build("{a}", null);
        Assert.assertTrue(expr instanceof MacroExpression);
        Assert.assertEquals("a", ((MacroExpression) expr).getMacro());

        Assert.assertEquals("1", expr.evaluate(name -> "a".equals(name) ? "1" : null));
    }
}
