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
import org.bithon.component.commons.expression.LiteralExpression;
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
    public void test_ComparisonFlip_Folding() {
        // Two literals will be folded
        IExpression expr = ExpressionASTBuilder.build("5 > 4", null);
        Assert.assertTrue(expr instanceof LiteralExpression);;
        Assert.assertTrue((Boolean) expr.evaluate(null));
    }

    @Test
    public void test_ComparisonFlip_GT() {
        IExpression expr = ExpressionASTBuilder.build("5 > a", null);
        Assert.assertEquals("a < 5", expr.serializeToText(null));

        Assert.assertFalse((boolean) expr.evaluate(a -> 6));
        Assert.assertFalse((boolean) expr.evaluate(a -> 5));
        Assert.assertTrue((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonFlip_GTE() {
        IExpression expr = ExpressionASTBuilder.build("5 >= a", null);
        Assert.assertEquals("a <= 5", expr.serializeToText(null));

        Assert.assertFalse((boolean) expr.evaluate(a -> 6));
        Assert.assertTrue((boolean) expr.evaluate(a -> 5));
        Assert.assertTrue((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonFlip_LT() {
        IExpression expr = ExpressionASTBuilder.build("5 < a", null);
        Assert.assertEquals("a > 5", expr.serializeToText(null));

        Assert.assertTrue((boolean) expr.evaluate(a -> 6));
        Assert.assertFalse((boolean) expr.evaluate(a -> 5));
        Assert.assertFalse((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonFlip_LTE() {
        IExpression expr = ExpressionASTBuilder.build("5 <= a", null);
        Assert.assertEquals("a >= 5", expr.serializeToText(null));

        Assert.assertTrue((boolean) expr.evaluate(a -> 6));
        Assert.assertTrue((boolean) expr.evaluate(a -> 5));
        Assert.assertFalse((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonFlip_NE() {
        IExpression expr = ExpressionASTBuilder.build("5 <> a", null);
        Assert.assertEquals("a <> 5", expr.serializeToText(null));

        Assert.assertTrue((boolean) expr.evaluate(a -> 6));
        Assert.assertFalse((boolean) expr.evaluate(a -> 5));
        Assert.assertTrue((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonFlip_EQ() {
        IExpression expr = ExpressionASTBuilder.build("5 = a", null);
        Assert.assertEquals("a = 5", expr.serializeToText(null));

        Assert.assertFalse((boolean) expr.evaluate(a -> 6));
        Assert.assertTrue((boolean) expr.evaluate(a -> 5));
        Assert.assertFalse((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonFlip_Non_Literal() {
        IExpression expr = ExpressionASTBuilder.build("a = b", null);
        Assert.assertEquals("a = b", expr.serializeToText(null));
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

    @Test
    public void testArithmeticExpression_Precedence() {
        Assert.assertEquals(7L, ExpressionASTBuilder.build("1 + 2 * 3").evaluate(null));
        Assert.assertEquals(-5L, ExpressionASTBuilder.build("1 - 2 * 3").evaluate(null));

        Assert.assertEquals(7L, ExpressionASTBuilder.build("2 * 3 + 1").evaluate(null));
        Assert.assertEquals(5L, ExpressionASTBuilder.build("2 * 3 - 1").evaluate(null));

        Assert.assertEquals(9L, ExpressionASTBuilder.build("(1 + 2) * 3").evaluate(null));
        Assert.assertEquals(10L, ExpressionASTBuilder.build("3 * 3 + 1").evaluate(null));

        Assert.assertEquals(9L, ExpressionASTBuilder.build("3 * 6 / 2").evaluate(null));

        Assert.assertEquals(4L, ExpressionASTBuilder.build("1 + 6 / 2").evaluate(null));
        Assert.assertEquals(-2L, ExpressionASTBuilder.build("1 - 6 / 2").evaluate(null));

        Assert.assertEquals(3L, ExpressionASTBuilder.build("2 + 3 * 4 / 2 - 5").evaluate(null));
        Assert.assertEquals(-2L, ExpressionASTBuilder.build("2 + 3 * 4 / 2 - 15 / 3 * 2").evaluate(null));
    }

    @Test
    public void testLogicalExpression_Precedence() {
        Assert.assertEquals(true, ExpressionASTBuilder.build("3 > 2 AND 4 > 3").evaluate(null));
        Assert.assertEquals(true, ExpressionASTBuilder.build("3 > 2 OR 4 > 3").evaluate(null));
        Assert.assertEquals(true, ExpressionASTBuilder.build("1 > 2 OR 4 > 3").evaluate(null));
        Assert.assertEquals(false, ExpressionASTBuilder.build("1 > 2 OR 1 > 3").evaluate(null));

        Assert.assertEquals(true, ExpressionASTBuilder.build("3 > 2 AND 4").evaluate(null));

        // equivalent to NOT( 3 > 2 AND 4 > 2)
        Assert.assertEquals(false, ExpressionASTBuilder.build("NOT 3 > 2 AND 4 > 2").evaluate(null));
    }
}
