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

package org.bithon.server.storage.common.expression.optimizer;

import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.function.Parameter;
import org.bithon.server.storage.common.expression.ExpressionASTBuilder;
import org.bithon.server.storage.datasource.builtin.Functions;
import org.bithon.server.storage.datasource.builtin.IFunctionProvider;
import org.junit.Assert;
import org.junit.Test;

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
                public String getName() {
                    return "sum";
                }

                @Override
                public List<Parameter> getParameters() {
                    return Arrays.asList(new Parameter(IDataType.LONG), new Parameter(IDataType.LONG));
                }

                @Override
                public void validateParameter(List<IExpression> parameters) {
                }

                @Override
                public Object evaluate(List<Object> parameters) {
                    return ((Number) parameters.get(0)).longValue() +
                        ((Number) parameters.get(1)).longValue();
                }
            };
        }
    }

    private final ExpressionASTBuilder expressionBuilder = ExpressionASTBuilder.builder().functions(new FunctionProvider());

    @Test
    public void testConstantFolding_Function() {
        IExpression expr = expressionBuilder.build("sum(1,2)");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals(3L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_RecursiveFunctionCall() {
        IExpression expr = expressionBuilder.build("sum(1,sum(2,3))");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals(6L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_AddExpression() {
        IExpression expr = expressionBuilder.build("1 + sum(1,sum(2,3))");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals(7L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_SubExpression() {
        IExpression expr = expressionBuilder.build("5 - 3");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals(2L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_MulExpression() {
        IExpression expr = expressionBuilder.build("5 * 3");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals(15L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_DivExpression() {
        IExpression expr = expressionBuilder.build("10 / 2");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals(5L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_ANDExpression() {
        IExpression expr = expressionBuilder.build("1 AND 1");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_ANDExpression_Bool_Bool() {
        IExpression expr = expressionBuilder.build("true AND true");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_ANDExpression_Bool_Bool_2() {
        IExpression expr = expressionBuilder.build("true AND false");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("false", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_ORExpression() {
        IExpression expr = expressionBuilder.build("1 OR 0");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_Comparison_GT() {
        IExpression expr = expressionBuilder.build("1 = 1");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_Comparison_GTE() {
        IExpression expr = expressionBuilder.build("1 >= 1");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_Comparison_LT() {
        IExpression expr = expressionBuilder.build("1 < 2");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void testConstantFolding_Comparison_LTE() {
        IExpression expr = expressionBuilder.build("1 <= 1");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText());
    }

    @Test
    public void test_NotReplaced() {
        IExpression expr = ExpressionASTBuilder.builder().functions(Functions.getInstance()).build("hasToken(a, 'ab')");

        Assert.assertEquals("hasToken(a, 'ab')", expr.serializeToText(null));
    }

    @Test
    public void test_RemoveAlwaysTrueCondition() {
        IExpression expr = ExpressionASTBuilder.builder().build("1 AND a = 'Good'");

        Assert.assertEquals("a = 'Good'", expr.serializeToText(null));
    }

    @Test
    public void test_RemoveAlwaysTrueCondition_2() {
        IExpression expr = ExpressionASTBuilder.builder().build("1 = 1 AND a = 'Good'");

        Assert.assertEquals("a = 'Good'", expr.serializeToText(null));
    }

    @Test
    public void test_RemoveAlwaysTrueCondition_3() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 'Good' AND 1 = 1");

        Assert.assertEquals("a = 'Good'", expr.serializeToText(null));
    }

    @Test
    public void test_RemoveAlwaysTrueCondition_4() {
        IExpression expr = ExpressionASTBuilder.builder().build("2 > 1 AND 1 = 1");

        Assert.assertEquals("true", expr.serializeToText(null));
    }

    @Test
    public void test_RemoveAlwaysFalseCondition() {
        IExpression expr = ExpressionASTBuilder.builder().build("0 AND a = 'God'");

        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("false", expr.serializeToText(null));
    }

    @Test
    public void test_RemoveAlwaysFalseCondition_2() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 'God' AND b = 'is' AND 'good' = 'bad'");

        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("false", expr.serializeToText(null));
    }

    @Test
    public void test_OptimizeOR_1() {
        IExpression expr = ExpressionASTBuilder.builder().build("1 OR a = 'Good'");

        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText(null));
    }

    @Test
    public void test_OptimizeOR_2() {
        IExpression expr = ExpressionASTBuilder.builder().build("1 = 1 OR a = 'Good'");

        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText(null));
    }

    @Test
    public void test_OptimizeOR_3() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 'Good' OR 1 = 1");

        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText(null));
    }

    @Test
    public void test_OptimizeOR_4() {
        IExpression expr = ExpressionASTBuilder.builder().build("2 > 1 OR 1 = 1");

        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText(null));
    }

    @Test
    public void test_OptimizeOR_5() {
        IExpression expr = ExpressionASTBuilder.builder().build("0 OR a = 'God'");

        Assert.assertEquals("a = 'God'", expr.serializeToText(null));
    }

    @Test
    public void test_OptimizeOR_6() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 'God' OR 1 > 2 OR b = 'c'");

        Assert.assertEquals("(a = 'God' OR b = 'c')", expr.serializeToText(null));
    }

    @Test
    public void test_OptimizeLogical() {
        IExpression expr = ExpressionASTBuilder.builder().build("a = 'God' OR 1 > 2 OR b = 'c' AND 1 = 1");

        Assert.assertEquals("(a = 'God' OR b = 'c')", expr.serializeToText(null));
    }

    @Test
    public void test_Optimize_NOT() {
        IExpression expr = ExpressionASTBuilder.builder().build("NOT 1 = 1");

        Assert.assertEquals("false", expr.serializeToText(null));
    }

    @Test
    public void test_Optimize_NOT_2() {
        IExpression expr = ExpressionASTBuilder.builder().build("NOT 1 > 1");

        Assert.assertEquals("true", expr.serializeToText(null));
    }

    @Test
    public void test_Optimize_NOT_3() {
        IExpression expr = ExpressionASTBuilder.builder().build("NOT 1 >= 1");

        Assert.assertEquals("false", expr.serializeToText(null));
    }

    @Test
    public void test_Optimize_NOT_4() {
        IExpression expr = ExpressionASTBuilder.builder().build("NOT (a = 'God' AND 0)");

        Assert.assertEquals("true", expr.serializeToText(null));
    }

    @Test
    public void test_Optimize_Constant() {
        IExpression expr = ExpressionASTBuilder.builder().build("1 AND 2");

        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText(null));
    }
}
