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

import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.function.IFunctionProvider;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * @author Frank Chen
 * @date 5/9/23 10:34 pm
 */
public class ExpressionOptimizerTest {

    static class FunctionProvider implements IFunctionProvider {

        @Override
        public IFunction getFunction(String name) {
            return new IFunction() {
                @Override
                public IDataType getReturnType() {
                    return IDataType.LONG;
                }

                @Override
                public boolean isDeterministic() {
                    return true;
                }

                @Override
                public String getName() {
                    return "sum";
                }

                @Override
                public List<IDataType> getParameterTypeList() {
                    return Arrays.asList(IDataType.LONG, IDataType.LONG);
                }

                @Override
                public void validateArgs(List<IExpression> args) {
                }

                @Override
                public Object evaluate(List<Object> args) {
                    return ((Number) args.get(0)).longValue() +
                           ((Number) args.get(1)).longValue();
                }
            };
        }
    }

    private final ExpressionASTBuilder expressionBuilder = ExpressionASTBuilder.builder().functions(new FunctionProvider());

    @Test
    public void testConstantFolding_Function() {
        IExpression expr = expressionBuilder.build("sum(1,2)");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals(3L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_RecursiveFunctionCall() {
        IExpression expr = expressionBuilder.build("sum(1,sum(2,3))");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals(6L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_AddExpression() {
        IExpression expr = expressionBuilder.build("1 + sum(1,sum(2,3))");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals(7L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_SubExpression() {
        IExpression expr = expressionBuilder.build("5 - 3");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals(2L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_MulExpression() {
        IExpression expr = expressionBuilder.build("5 * 3");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals(15L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_DivExpression() {
        IExpression expr = expressionBuilder.build("10 / 2");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals(5L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_ANDExpression() {
        IExpression expr = expressionBuilder.build("1 AND 1");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_ANDExpression_Bool_Bool() {
        IExpression expr = expressionBuilder.build("true AND true");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_ANDExpression_Bool_Bool_2() {
        IExpression expr = expressionBuilder.build("true AND false");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("false", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_ANDExpression_Bool_Bool_3() {
        IExpression expr = expressionBuilder.build("false AND false");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("false", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_ORExpression() {
        IExpression expr = expressionBuilder.build("1 OR 0");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_Comparison_GT() {
        IExpression expr = expressionBuilder.build("1 = 1");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_Comparison_GTE() {
        IExpression expr = expressionBuilder.build("1 >= 1");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_Comparison_LT() {
        IExpression expr = expressionBuilder.build("1 < 2");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_Comparison_LTE() {
        IExpression expr = expressionBuilder.build("1 <= 1");
        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void test_NotReplaced() {
        IExpression expr = ExpressionASTBuilder.builder().functions(Functions.getInstance()).build("hasToken(a, 'ab')");

        Assertions.assertEquals("hasToken(a, 'ab')", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_RemoveAlwaysTrueCondition() {
        IExpression expr = ExpressionASTBuilder.builder().build("1 AND a = 'Good'");

        Assertions.assertEquals("a = 'Good'", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_RemoveAlwaysTrueCondition_2() {
        IExpression expr = ExpressionASTBuilder.builder().build("1 = 1 AND a = 'Good'");

        Assertions.assertEquals("a = 'Good'", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_RemoveAlwaysTrueCondition_3() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 'Good' AND 1 = 1");

        Assertions.assertEquals("a = 'Good'", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_RemoveAlwaysTrueCondition_4() {
        IExpression expr = ExpressionASTBuilder.builder().build("2 > 1 AND 1 = 1");

        Assertions.assertEquals("true", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_RemoveAlwaysFalseCondition() {
        IExpression expr = ExpressionASTBuilder.builder().build("0 AND a = 'God'");

        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("false", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_RemoveAlwaysFalseCondition_2() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 'God' AND b = 'is' AND 'good' = 'bad'");

        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("false", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_OptimizeOR_1() {
        IExpression expr = ExpressionASTBuilder.builder().build("1 OR a = 'Good'");

        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("true", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_OptimizeOR_2() {
        IExpression expr = ExpressionASTBuilder.builder().build("1 = 1 OR a = 'Good'");

        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("true", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_OptimizeOR_3() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 'Good' OR 1 = 1");

        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("true", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_OptimizeOR_4() {
        IExpression expr = ExpressionASTBuilder.builder().build("2 > 1 OR 1 = 1");

        Assertions.assertInstanceOf(LiteralExpression.BooleanLiteral.class, expr);
        Assertions.assertEquals("true", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_OptimizeOR_5() {
        IExpression expr = ExpressionASTBuilder.builder().build("0 OR a = 'God'");

        Assertions.assertEquals("a = 'God'", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_OptimizeOR_6() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 'God' OR 1 > 2 OR b = 'c'");

        Assertions.assertEquals("(a = 'God') OR (b = 'c')", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_OptimizeLogical() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 'God' OR 1 > 2 OR b = 'c' AND 1 = 1");

        Assertions.assertEquals("(a = 'God') OR (b = 'c')", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Optimize_Constant_NOT() {
        IExpression expr = ExpressionASTBuilder.builder().build("NOT 1 = 1");

        Assertions.assertEquals("false", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Optimize_Constant_NOT_2() {
        IExpression expr = ExpressionASTBuilder.builder().build("NOT 1 > 1");

        Assertions.assertEquals("true", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Optimize_Constant_NOT_3() {
        IExpression expr = ExpressionASTBuilder.builder().build("NOT 1 >= 1");

        Assertions.assertEquals("false", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Optimize_Constant_NOT_4() {
        IExpression expr = ExpressionASTBuilder.builder().build("NOT (a = 'God' AND 0)");

        Assertions.assertEquals("true", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Optimize_Constant_NOT_IN() {
        IExpression expr = ExpressionASTBuilder.builder().build("NOT (1 in (1))");

        Assertions.assertEquals("false", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Optimize_Constant_NOT_NOT_IN() {
        IExpression expr = ExpressionASTBuilder.builder().build("NOT (1 not in (1))");

        Assertions.assertEquals("true", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Optimize_Constant() {
        IExpression expr = ExpressionASTBuilder.builder().build("1 AND 2");

        Assertions.assertInstanceOf(LiteralExpression.class, expr);
        Assertions.assertEquals("true", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Not_NotIn() {
        IExpression expr = ExpressionASTBuilder.builder().build("not a not in (1,2,3)");
        Assertions.assertEquals("a in (1, 2, 3)", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Not_NotLike() {
        IExpression expr = ExpressionASTBuilder.builder().build("not a not contains 'a'");
        Assertions.assertEquals("a contains 'a'", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Not_EQ() {
        IExpression expr = ExpressionASTBuilder.builder().build("not a = b");
        Assertions.assertEquals("a <> b", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Not_NE() {
        IExpression expr = ExpressionASTBuilder.builder().build("not a <> b");
        Assertions.assertEquals("a = b", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Not_GT() {
        IExpression expr = ExpressionASTBuilder.builder().build("not a > b");
        Assertions.assertEquals("a <= b", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Not_GTE() {
        IExpression expr = ExpressionASTBuilder.builder().build("not a >= b");
        Assertions.assertEquals("a < b", expr.serializeToText(IdentifierQuotaStrategy.NONE));

    }

    @Test
    public void test_Not_LT() {
        IExpression expr = ExpressionASTBuilder.builder().build("not a < b");
        Assertions.assertEquals("a >= b", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Not_LTE() {
        IExpression expr = ExpressionASTBuilder.builder().build("not a <= b");
        Assertions.assertEquals("a > b", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_Optimize_Now_Function() {
        {
            long prev = System.currentTimeMillis() / 1000 - 5;
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .functions(Functions.getInstance())
                                                   .build("now() - 5s");
            Assertions.assertInstanceOf(LiteralExpression.LongLiteral.class, expr);

            long diff = Math.abs(((LiteralExpression.LongLiteral) expr).getValue() - prev);

            // To avoid flaky, we think that 3 seconds are enough for evaluation above code
            Assertions.assertTrue(diff < 3);
        }

        {
            long prev = System.currentTimeMillis() / 1000 - Duration.ofDays(5).getSeconds();
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .functions(Functions.getInstance())
                                                   .build("now() - 5d");
            Assertions.assertInstanceOf(LiteralExpression.LongLiteral.class, expr);

            long diff = Math.abs(((LiteralExpression.LongLiteral) expr).getValue() - prev);

            // To avoid flaky, we think that 3 seconds are enough for evaluation above code
            Assertions.assertTrue(diff < 3);
        }
    }
}
