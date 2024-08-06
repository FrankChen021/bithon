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
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.function.Functions;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/8/19 17:23
 */
public class ExpressionASTBuilderTest {

    @Test
    public void testIn() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 in (1,2)");
        Assert.assertFalse((Boolean) expr.evaluate(null));
    }

    @Test
    public void testIn_2() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 in (5)");
        Assert.assertTrue((Boolean) expr.evaluate(null));
    }

    @Test
    public void testIn_3() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 in (5,6)");
        Assert.assertTrue((Boolean) expr.evaluate(null));
    }

    @Test
    public void test_ComparisonFlip_Folding() {
        // Two literals will be folded
        IExpression expr = ExpressionASTBuilder.builder().build("5 > 4");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertTrue((Boolean) expr.evaluate(null));
    }

    @Test
    public void test_ComparisonFlip_GT() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 > a");
        Assert.assertEquals("a < 5", expr.serializeToText(null));

        Assert.assertFalse((boolean) expr.evaluate(a -> 6));
        Assert.assertFalse((boolean) expr.evaluate(a -> 5));
        Assert.assertTrue((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonFlip_GTE() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 >= a");
        Assert.assertEquals("a <= 5", expr.serializeToText(null));

        Assert.assertFalse((boolean) expr.evaluate(a -> 6));
        Assert.assertTrue((boolean) expr.evaluate(a -> 5));
        Assert.assertTrue((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonFlip_LT() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 < a");
        Assert.assertEquals("a > 5", expr.serializeToText(null));

        Assert.assertTrue((boolean) expr.evaluate(a -> 6));
        Assert.assertFalse((boolean) expr.evaluate(a -> 5));
        Assert.assertFalse((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonFlip_LTE() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 <= a");
        Assert.assertEquals("a >= 5", expr.serializeToText(null));

        Assert.assertTrue((boolean) expr.evaluate(a -> 6));
        Assert.assertTrue((boolean) expr.evaluate(a -> 5));
        Assert.assertFalse((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonFlip_NE() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 <> a");
        Assert.assertEquals("a <> 5", expr.serializeToText(null));

        Assert.assertTrue((boolean) expr.evaluate(a -> 6));
        Assert.assertFalse((boolean) expr.evaluate(a -> 5));
        Assert.assertTrue((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonFlip_EQ() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 = a");
        Assert.assertEquals("a = 5", expr.serializeToText(null));

        Assert.assertFalse((boolean) expr.evaluate(a -> 6));
        Assert.assertTrue((boolean) expr.evaluate(a -> 5));
        Assert.assertFalse((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonFlip_Non_Literal() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = b");
        Assert.assertEquals("a = b", expr.serializeToText(null));
    }

    @Test
    public void testLogical_ConsecutiveAND() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1 AND b = 1 AND c = 1 AND d = 1");
        Assert.assertTrue(expr instanceof LogicalExpression.AND);
        Assert.assertEquals(4, ((LogicalExpression.AND) expr).getOperands().size());
        for (int i = 0; i < 4; i++) {
            Assert.assertTrue(((LogicalExpression.AND) expr).getOperands().get(i) instanceof ComparisonExpression);
        }
    }

    @Test
    public void testLogical_OR() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1 OR b = 1");
        Assert.assertTrue(expr instanceof LogicalExpression.OR);
        Assert.assertEquals("OR", ((LogicalExpression.OR) expr).getOperator());
    }

    @Test
    public void testLogical_ConsecutiveOR() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1 OR b = 1 OR c = 1 OR d = 1");
        Assert.assertTrue(expr instanceof LogicalExpression.OR);
        Assert.assertEquals(4, ((LogicalExpression.OR) expr).getOperands().size());
        for (int i = 0; i < 4; i++) {
            Assert.assertTrue(((LogicalExpression.OR) expr).getOperands().get(i) instanceof ComparisonExpression);
        }
    }

    @Test
    public void testLogical_AND_OR() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1 AND b = 1 AND c = 1 OR d = 1");
        Assert.assertTrue(expr instanceof LogicalExpression.OR);
        Assert.assertEquals(2, ((LogicalExpression.OR) expr).getOperands().size());

        Assert.assertTrue(((LogicalExpression.OR) expr).getOperands().get(0) instanceof LogicalExpression.AND);
        Assert.assertTrue(((LogicalExpression.OR) expr).getOperands().get(1) instanceof ComparisonExpression);
    }

    @Test
    public void testArithmeticExpression_Precedence() {
        Assert.assertEquals(7L, ExpressionASTBuilder.builder().build("1 + 2 * 3").evaluate(null));
        Assert.assertEquals(-5L, ExpressionASTBuilder.builder().build("1 - 2 * 3").evaluate(null));

        Assert.assertEquals(7L, ExpressionASTBuilder.builder().build("2 * 3 + 1").evaluate(null));
        Assert.assertEquals(5L, ExpressionASTBuilder.builder().build("2 * 3 - 1").evaluate(null));

        Assert.assertEquals(9L, ExpressionASTBuilder.builder().build("(1 + 2) * 3").evaluate(null));
        Assert.assertEquals(10L, ExpressionASTBuilder.builder().build("3 * 3 + 1").evaluate(null));

        Assert.assertEquals(9L, ExpressionASTBuilder.builder().build("3 * 6 / 2").evaluate(null));

        Assert.assertEquals(4L, ExpressionASTBuilder.builder().build("1 + 6 / 2").evaluate(null));
        Assert.assertEquals(-2L, ExpressionASTBuilder.builder().build("1 - 6 / 2").evaluate(null));

        Assert.assertEquals(3L, ExpressionASTBuilder.builder().build("2 + 3 * 4 / 2 - 5").evaluate(null));
        Assert.assertEquals(-2L, ExpressionASTBuilder.builder().build("2 + 3 * 4 / 2 - 15 / 3 * 2").evaluate(null));
    }

    @Test
    public void testLogicalExpression_Precedence() {
        Assert.assertEquals(true, ExpressionASTBuilder.builder().build("3 > 2 AND 4 > 3").evaluate(null));
        Assert.assertEquals(true, ExpressionASTBuilder.builder().build("3 > 2 OR 4 > 3").evaluate(null));
        Assert.assertEquals(true, ExpressionASTBuilder.builder().build("1 > 2 OR 4 > 3").evaluate(null));
        Assert.assertEquals(false, ExpressionASTBuilder.builder().build("1 > 2 OR 1 > 3").evaluate(null));

        Assert.assertEquals(true, ExpressionASTBuilder.builder().build("3 > 2 AND 4").evaluate(null));

        // equivalent to NOT( 3 > 2 AND 4 > 2)
        Assert.assertEquals(false, ExpressionASTBuilder.builder().build("NOT 3 > 2 AND 4 > 2").evaluate(null));
    }

    @Test
    public void test_EscapeSingleQuote() {
        Assert.assertEquals("message like 'a\\''", ExpressionASTBuilder.builder().build("message LIKE 'a\\''").serializeToText(null));
    }

    @Test
    public void test_TernaryExpression() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .build("a > b ? 1 : 2");

        Assert.assertEquals("a > b ? 1 : 2", expr.serializeToText(null));
        long v = (long) expr.evaluate(name -> {
            if ("a".equals(name)) {
                return 4;
            }
            return 1;
        });
        Assert.assertEquals(1L, v);
    }

    @Test
    public void test_Is_Null_Expression() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .build("a is null");

        Assert.assertEquals("a IS null", expr.serializeToText(null));

        Assert.assertTrue((boolean) expr.evaluate((name) -> null));
        Assert.assertFalse((boolean) expr.evaluate((name) -> "value of a"));
    }

    @Test
    public void test_CountAggregation() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .build("count(1)");

        Assert.assertEquals("count(1)", expr.serializeToText(null));
    }

    @Test
    public void test_DurationLiteral() {
        {
            IExpression expr = ExpressionASTBuilder.builder().build("a >= 5m");
            Assert.assertEquals("a >= 5m", expr.serializeToText(null));
            Assert.assertTrue((boolean) expr.evaluate((name) -> 301));
            Assert.assertTrue((boolean) expr.evaluate((name) -> 300));
            Assert.assertFalse((boolean) expr.evaluate((name) -> 299));
        }

        {
            IExpression expr = ExpressionASTBuilder.builder().build("1h");
            Assert.assertEquals("1h", expr.serializeToText(null));
            Assert.assertEquals(1, ((LiteralExpression.ReadableDurationLiteral) expr).getLiteralValue().getDuration().toHours());
        }
        {
            IExpression expr = ExpressionASTBuilder.builder().build("7d");
            Assert.assertEquals("7d", expr.serializeToText(null));
            Assert.assertEquals(7, ((LiteralExpression.ReadableDurationLiteral) expr).getLiteralValue().getDuration().toDays());
        }
        {
            IExpression expr = ExpressionASTBuilder.builder().build("69s");
            Assert.assertEquals("69s", expr.serializeToText(null));
            Assert.assertEquals(69, ((LiteralExpression.ReadableDurationLiteral) expr).getLiteralValue().getDuration().toSeconds());
        }
    }

    @Test
    public void test_PercentageLiteral() {
        IExpression expr = ExpressionASTBuilder.builder().build("a >= 5%");
        Assert.assertEquals("a >= 5%", expr.serializeToText(null));

        Assert.assertTrue((boolean) expr.evaluate((name) -> 0.051f));
        Assert.assertTrue((boolean) expr.evaluate((name) -> 0.05f));
        Assert.assertFalse((boolean) expr.evaluate((name) -> 0.049));
    }

    @Test
    public void test_SizeLiteral() {
        IExpression expr = ExpressionASTBuilder.builder().build("a >= 5Ki");
        Assert.assertEquals("a >= 5Ki", expr.serializeToText(null));
        Assert.assertTrue((boolean) expr.evaluate((name) -> 5 * 1024 + 1));
        Assert.assertTrue((boolean) expr.evaluate((name) -> 5 * 1024));
        Assert.assertFalse((boolean) expr.evaluate((name) -> 5 * 1024 - 1));

        Assert.assertEquals("a >= 5K", ExpressionASTBuilder.builder().build("a >= 5K").serializeToText(null));
        Assert.assertEquals("a >= 5KiB", ExpressionASTBuilder.builder().build("a >= 5KiB").serializeToText(null));
        Assert.assertEquals("a >= 5G", ExpressionASTBuilder.builder().build("a >= 5G").serializeToText(null));
        Assert.assertEquals("a >= 5Gi", ExpressionASTBuilder.builder().build("a >= 5Gi").serializeToText(null));
        Assert.assertEquals("a >= 5GiB", ExpressionASTBuilder.builder().build("a >= 5GiB").serializeToText(null));
        Assert.assertEquals("a >= 5M", ExpressionASTBuilder.builder().build("a >= 5M").serializeToText(null));
        Assert.assertEquals("a >= 5Mi", ExpressionASTBuilder.builder().build("a >= 5Mi").serializeToText(null));
        Assert.assertEquals("a >= 5MiB", ExpressionASTBuilder.builder().build("a >= 5MiB").serializeToText(null));
        Assert.assertEquals("a >= 5P", ExpressionASTBuilder.builder().build("a >= 5P").serializeToText(null));
        Assert.assertEquals("a >= 5Pi", ExpressionASTBuilder.builder().build("a >= 5Pi").serializeToText(null));
        Assert.assertEquals("a >= 5PiB", ExpressionASTBuilder.builder().build("a >= 5PiB").serializeToText(null));
    }

    @Test
    public void test_LikeExpressionSyntaxError() {
        // %l if it's not properly treated will be considered as format string which is invalid
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder().build("tags['exceptionCode'] = '60' AND and tags['statement'] like '%live%'"));
    }

    @Test
    public void test_NowExpression() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .optimizationEnabled(false)
                                               .build("now() - 5s");
        Assert.assertEquals("now() - 5s", expr.serializeToText());
    }
}
