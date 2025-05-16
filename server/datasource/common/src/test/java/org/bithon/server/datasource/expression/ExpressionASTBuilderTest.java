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

package org.bithon.server.datasource.expression;

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author frank.chen021@outlook.com
 * @date 2023/8/19 17:23
 */
public class ExpressionASTBuilderTest {

    @Test
    public void test_InExpression() {
        {
            IExpression expr = ExpressionASTBuilder.builder().build("5 in (1,2)");
            Assertions.assertFalse((Boolean) expr.evaluate(null));
        }
        {
            IExpression expr = ExpressionASTBuilder.builder().build("5 in (5)");
            Assertions.assertTrue((Boolean) expr.evaluate(null));
        }
        {
            IExpression expr = ExpressionASTBuilder.builder().build("5 in (5,6)");
            Assertions.assertTrue((Boolean) expr.evaluate(null));
        }
    }

    @Test
    public void test_ComparisonExpression_ConstantFolding() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 > 4");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertTrue((Boolean) expr.evaluate(null));
    }

    @Test
    public void test_ComparisonExpression_FlipGT() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 > a");
        Assertions.assertEquals("a < 5", expr.serializeToText(IdentifierQuotaStrategy.NONE));

        Assertions.assertFalse((boolean) expr.evaluate(a -> 6));
        Assertions.assertFalse((boolean) expr.evaluate(a -> 5));
        Assertions.assertTrue((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonExpression_FlipGTE() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 >= a");
        Assertions.assertEquals("a <= 5", expr.serializeToText(IdentifierQuotaStrategy.NONE));

        Assertions.assertFalse((boolean) expr.evaluate(a -> 6));
        Assertions.assertTrue((boolean) expr.evaluate(a -> 5));
        Assertions.assertTrue((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonExpression_FlipLT() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 < a");
        Assertions.assertEquals("a > 5", expr.serializeToText(IdentifierQuotaStrategy.NONE));

        Assertions.assertTrue((boolean) expr.evaluate(a -> 6));
        Assertions.assertFalse((boolean) expr.evaluate(a -> 5));
        Assertions.assertFalse((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonExpression_FlipLTE() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 <= a");
        Assertions.assertEquals("a >= 5", expr.serializeToText(IdentifierQuotaStrategy.NONE));

        Assertions.assertTrue((boolean) expr.evaluate(a -> 6));
        Assertions.assertTrue((boolean) expr.evaluate(a -> 5));
        Assertions.assertFalse((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonExpression_FlipNE() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 <> a");
        Assertions.assertEquals("a <> 5", expr.serializeToText(IdentifierQuotaStrategy.NONE));

        Assertions.assertTrue((boolean) expr.evaluate(a -> 6));
        Assertions.assertFalse((boolean) expr.evaluate(a -> 5));
        Assertions.assertTrue((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonExpression_Flip_EQ() {
        IExpression expr = ExpressionASTBuilder.builder().build("5 = a");
        Assertions.assertEquals("a = 5", expr.serializeToText(IdentifierQuotaStrategy.NONE));

        Assertions.assertFalse((boolean) expr.evaluate(a -> 6));
        Assertions.assertTrue((boolean) expr.evaluate(a -> 5));
        Assertions.assertFalse((boolean) expr.evaluate(a -> 4));
    }

    @Test
    public void test_ComparisonExpression_Flip_Non_Literal() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = b");
        Assertions.assertEquals("a = b", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_LogicalExpression_ConsecutiveAND() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1 AND b = 1 AND c = 1 AND d = 1");
        Assertions.assertInstanceOf(LogicalExpression.AND.class, expr);
        Assertions.assertEquals(4, ((LogicalExpression.AND) expr).getOperands().size());
        for (int i = 0; i < 4; i++) {
            Assertions.assertInstanceOf(ComparisonExpression.class,
                                        ((LogicalExpression.AND) expr).getOperands().get(i));
        }
    }

    @Test
    public void test_LogicalExpression_ConsecutiveAND_WithBrace() {
        IExpression expr = ExpressionASTBuilder.builder().build("(a = 1) AND (b = 1 AND c = 1 AND d = 1)");
        Assertions.assertInstanceOf(LogicalExpression.AND.class, expr);

        // Logical AND is flattened
        Assertions.assertEquals(4, ((LogicalExpression.AND) expr).getOperands().size());
        Assertions.assertEquals("(a = 1) AND (b = 1) AND (c = 1) AND (d = 1)", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_LogicalExpression_OR() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1 OR b = 1");
        Assertions.assertInstanceOf(LogicalExpression.OR.class, expr);
        Assertions.assertEquals("OR", ((LogicalExpression.OR) expr).getOperator());
    }

    @Test
    public void test_LogicalExpression_ConsecutiveOR() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1 OR b = 1 OR c = 1 OR d = 1");
        Assertions.assertInstanceOf(LogicalExpression.OR.class, expr);
        Assertions.assertEquals(4, ((LogicalExpression.OR) expr).getOperands().size());
        for (int i = 0; i < 4; i++) {
            Assertions.assertInstanceOf(ComparisonExpression.class, ((LogicalExpression.OR) expr).getOperands().get(i));
        }
    }

    @Test
    public void test_LogicalExpression_AND_OR() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 1 AND b = 1 AND c = 1 OR d = 1");
        Assertions.assertInstanceOf(LogicalExpression.OR.class, expr);
        Assertions.assertEquals(2, ((LogicalExpression.OR) expr).getOperands().size());

        Assertions.assertInstanceOf(LogicalExpression.AND.class, ((LogicalExpression.OR) expr).getOperands().get(0));
        Assertions.assertInstanceOf(ComparisonExpression.class, ((LogicalExpression.OR) expr).getOperands().get(1));
    }

    @Test
    public void test_ArithmeticExpression_Precedence() {
        Assertions.assertEquals(7L, ExpressionASTBuilder.builder().build("1 + 2 * 3").evaluate(null));
        Assertions.assertEquals(-5L, ExpressionASTBuilder.builder().build("1 - 2 * 3").evaluate(null));

        Assertions.assertEquals(7L, ExpressionASTBuilder.builder().build("2 * 3 + 1").evaluate(null));
        Assertions.assertEquals(5L, ExpressionASTBuilder.builder().build("2 * 3 - 1").evaluate(null));

        Assertions.assertEquals(9L, ExpressionASTBuilder.builder().build("(1 + 2) * 3").evaluate(null));
        Assertions.assertEquals(10L, ExpressionASTBuilder.builder().build("3 * 3 + 1").evaluate(null));

        Assertions.assertEquals(9L, ExpressionASTBuilder.builder().build("3 * 6 / 2").evaluate(null));

        Assertions.assertEquals(4L, ExpressionASTBuilder.builder().build("1 + 6 / 2").evaluate(null));
        Assertions.assertEquals(-2L, ExpressionASTBuilder.builder().build("1 - 6 / 2").evaluate(null));

        Assertions.assertEquals(3L, ExpressionASTBuilder.builder().build("2 + 3 * 4 / 2 - 5").evaluate(null));
        Assertions.assertEquals(-2L, ExpressionASTBuilder.builder().build("2 + 3 * 4 / 2 - 15 / 3 * 2").evaluate(null));
    }

    @Test
    public void test_LogicalExpression_Precedence() {
        Assertions.assertEquals(true, ExpressionASTBuilder.builder().build("3 > 2 AND 4 > 3").evaluate(null));
        Assertions.assertEquals(true, ExpressionASTBuilder.builder().build("3 > 2 OR 4 > 3").evaluate(null));
        Assertions.assertEquals(true, ExpressionASTBuilder.builder().build("1 > 2 OR 4 > 3").evaluate(null));
        Assertions.assertEquals(false, ExpressionASTBuilder.builder().build("1 > 2 OR 1 > 3").evaluate(null));

        Assertions.assertEquals(true, ExpressionASTBuilder.builder().build("3 > 2 AND 4").evaluate(null));

        // equivalent to NOT( 3 > 2 AND 4 > 2)
        Assertions.assertEquals(false, ExpressionASTBuilder.builder().build("NOT 3 > 2 AND 4 > 2").evaluate(null));
    }

    @Test
    public void test_StringLiteralUnEscaped() {
        // At the AST layer, the string literal does not hold escape characters.
        // The business layer (the layer that uses the AST) needs to handle the escaping
        Assertions.assertEquals("message contains 'a''", ExpressionASTBuilder.builder()
                                                                         .build("message contains 'a\\''")
                                                                         .serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_TernaryExpression() {
        IExpression expr = ExpressionASTBuilder.builder().build("a > b ? 1 : 2");

        Assertions.assertEquals("a > b ? 1 : 2", expr.serializeToText(IdentifierQuotaStrategy.NONE));
        long v = (long) expr.evaluate(name -> {
            if ("a".equals(name)) {
                return 4;
            }
            return 1;
        });
        Assertions.assertEquals(1L, v);
    }

    @Test
    public void test_IsNullExpression() {
        IExpression expr = ExpressionASTBuilder.builder().build("a is null");

        Assertions.assertEquals("a IS null", expr.serializeToText(IdentifierQuotaStrategy.NONE));

        Assertions.assertTrue((boolean) expr.evaluate((name) -> null));
        Assertions.assertFalse((boolean) expr.evaluate((name) -> "value of a"));
    }

    @Test
    public void test_LiteralExpression_DurationLiteral() {
        {
            IExpression expr = ExpressionASTBuilder.builder().build("a >= 5m");
            Assertions.assertEquals("a >= 5m", expr.serializeToText(IdentifierQuotaStrategy.NONE));
            Assertions.assertTrue((boolean) expr.evaluate((name) -> 301));
            Assertions.assertTrue((boolean) expr.evaluate((name) -> 300));
            Assertions.assertFalse((boolean) expr.evaluate((name) -> 299));
        }

        {
            IExpression expr = ExpressionASTBuilder.builder().build("1h");
            Assertions.assertEquals("1h", expr.serializeToText(IdentifierQuotaStrategy.NONE));
            Assertions.assertEquals(1, ((LiteralExpression.ReadableDurationLiteral) expr).getValue().getDuration().toHours());
        }
        {
            IExpression expr = ExpressionASTBuilder.builder().build("7d");
            Assertions.assertEquals("7d", expr.serializeToText(IdentifierQuotaStrategy.NONE));
            Assertions.assertEquals(7, ((LiteralExpression.ReadableDurationLiteral) expr).getValue().getDuration().toDays());
        }
        {
            IExpression expr = ExpressionASTBuilder.builder().build("69s");
            Assertions.assertEquals("69s", expr.serializeToText(IdentifierQuotaStrategy.NONE));
            Assertions.assertEquals(69, ((LiteralExpression.ReadableDurationLiteral) expr).getValue().getDuration().toSeconds());
        }
        {
            IExpression expr = ExpressionASTBuilder.builder().build("69s.toMilliSeconds");

            // The 69s.toMilli is interpreted as toMilliSeconds(69s), which has been optimized to 69_000
            Assertions.assertEquals("69000", expr.serializeToText(IdentifierQuotaStrategy.NONE));
        }
        {
            IExpression expr = ExpressionASTBuilder.builder().build("1d .toMicroSeconds");

            // The 69s.toMicro is interpreted as toMicroSeconds(1d), which has been optimized to 86400_000_000
            Assertions.assertEquals("86400000000", expr.serializeToText(IdentifierQuotaStrategy.NONE));
        }
        {
            IExpression expr = ExpressionASTBuilder.builder().build("3m. toNanoSeconds");

            // The 69s.toNano is interpreted as toNanoSeconds(3m), which has been optimized to 180_000_000_000
            Assertions.assertEquals("180000000000", expr.serializeToText(IdentifierQuotaStrategy.NONE));
        }
    }

    @Test
    public void test_LiteralExpression_PercentageLiteral() {
        IExpression expr = ExpressionASTBuilder.builder().build("a >= 5%");
        Assertions.assertEquals("a >= 5%", expr.serializeToText(IdentifierQuotaStrategy.NONE));

        Assertions.assertTrue((boolean) expr.evaluate((name) -> 0.051f));
        Assertions.assertTrue((boolean) expr.evaluate((name) -> 0.05f));
        Assertions.assertFalse((boolean) expr.evaluate((name) -> 0.049));
    }

    @Test
    public void test_LiteralExpression_SizeLiteral() {
        IExpression expr = ExpressionASTBuilder.builder().build("a >= 5Ki");
        Assertions.assertEquals("a >= 5Ki", expr.serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertTrue((boolean) expr.evaluate((name) -> 5 * 1024 + 1));
        Assertions.assertTrue((boolean) expr.evaluate((name) -> 5 * 1024));
        Assertions.assertFalse((boolean) expr.evaluate((name) -> 5 * 1024 - 1));

        Assertions.assertEquals("a >= 5K", ExpressionASTBuilder.builder().build("a >= 5K").serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("a >= 5KiB", ExpressionASTBuilder.builder().build("a >= 5KiB").serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("a >= 5G", ExpressionASTBuilder.builder().build("a >= 5G").serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("a >= 5Gi", ExpressionASTBuilder.builder().build("a >= 5Gi").serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("a >= 5GiB", ExpressionASTBuilder.builder().build("a >= 5GiB").serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("a >= 5M", ExpressionASTBuilder.builder().build("a >= 5M").serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("a >= 5Mi", ExpressionASTBuilder.builder().build("a >= 5Mi").serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("a >= 5MiB", ExpressionASTBuilder.builder().build("a >= 5MiB").serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("a >= 5P", ExpressionASTBuilder.builder().build("a >= 5P").serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("a >= 5Pi", ExpressionASTBuilder.builder().build("a >= 5Pi").serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("a >= 5PiB", ExpressionASTBuilder.builder().build("a >= 5PiB").serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_LikeExpression_SyntaxError() {
        // if the '%l' is not properly treated, it will be considered as a format string which is invalid
        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder().build("tags['exceptionCode'] = '60' AND and tags['statement'] like '%live%'"));
    }

    @Test
    public void test_NowExpression() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .optimizationEnabled(false)
                                               .build("now() - 5s");
        Assertions.assertEquals("now() - 5s", expr.serializeToText());
    }

    @Test
    public void test_LiteralExpression_UnderscoreInteger() {
        Assertions.assertEquals("1", ExpressionASTBuilder.builder().build("1").serializeToText());
        Assertions.assertEquals("12", ExpressionASTBuilder.builder().build("12").serializeToText());
        Assertions.assertEquals("123", ExpressionASTBuilder.builder().build("123").serializeToText());
        Assertions.assertEquals("12", ExpressionASTBuilder.builder().build("1_2").serializeToText());
        Assertions.assertEquals("12", ExpressionASTBuilder.builder().build("1__2").serializeToText());

        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder().build("1__"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder().build("1_"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder().build("_1"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder().build("__1"));
    }

    @Test
    public void test_LiteralExpression_UnderscoreDecimal() {
        Assertions.assertEquals("1.0", ExpressionASTBuilder.builder().build("1.").serializeToText());
        Assertions.assertEquals("12.0", ExpressionASTBuilder.builder().build("12.").serializeToText());
        Assertions.assertEquals("123.0", ExpressionASTBuilder.builder().build("123.").serializeToText());
        Assertions.assertEquals("123.0", ExpressionASTBuilder.builder().build("12_3.").serializeToText());
        Assertions.assertEquals("123.0", ExpressionASTBuilder.builder().build("1_2_3.").serializeToText());

        Assertions.assertEquals("11.1", ExpressionASTBuilder.builder().build("1_1.1").serializeToText());
        Assertions.assertEquals("11.12", ExpressionASTBuilder.builder().build("1_1.12").serializeToText());
        Assertions.assertEquals("11.123", ExpressionASTBuilder.builder().build("1_1.123").serializeToText());

        Assertions.assertEquals("11.123", ExpressionASTBuilder.builder().build("1_1.1_23").serializeToText());
        Assertions.assertEquals("11.123", ExpressionASTBuilder.builder().build("1_1.1_2_3").serializeToText());

        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder().build("__1."));
        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder().build("_1."));
        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder().build("1_."));
        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder().build("11._"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder().build("11.2_"));
    }

    @Test
    public void test_RegexMatchOperator() {
        Assertions.assertEquals("\"a\" =~ 'a'", ExpressionASTBuilder.builder().build("a =~ 'a'").serializeToText());

        // rhs must be a string literal
        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder().build("a =~ b"));
    }

    @Test
    public void test_NotRegexMatchOperator() {
        Assertions.assertEquals("\"a\" !~ 'a'", ExpressionASTBuilder.builder().build("a !~ 'a'").serializeToText());

        // rhs must be a string literal
        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder().build("a !~ b"));
    }
}
