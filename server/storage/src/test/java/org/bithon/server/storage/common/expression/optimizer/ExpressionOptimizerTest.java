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

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.function.IDataType;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.function.Parameter;
import org.bithon.server.storage.common.expression.ExpressionASTBuilder;
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

    @Test
    public void testConstantFolding_Function() {
        IExpression expr = ExpressionASTBuilder.build("sum(1,2)", new FunctionProvider());
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals(3L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_RecursiveFunctionCall() {
        IExpression expr = ExpressionASTBuilder.build("sum(1,sum(2,3))", new FunctionProvider());
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals(6L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_AddExpression() {
        IExpression expr = ExpressionASTBuilder.build("1 + sum(1,sum(2,3))", new FunctionProvider());
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals(7L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_SubExpression() {
        IExpression expr = ExpressionASTBuilder.build("5 - 3", new FunctionProvider());
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals(2L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_MulExpression() {
        IExpression expr = ExpressionASTBuilder.build("5 * 3", new FunctionProvider());
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals(15L, expr.evaluate(null));
    }

    @Test
    public void testConstantFolding_DivExpression() {
        IExpression expr = ExpressionASTBuilder.build("10 / 2", new FunctionProvider());
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals(5L, expr.evaluate(null));
    }
    
    @Test
    public void testConstantFolding_ANDExpression() {
        IExpression expr = ExpressionASTBuilder.build("1 AND 1");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText(false));
    }

    @Test
    public void testConstantFolding_ORExpression() {
        IExpression expr = ExpressionASTBuilder.build("1 OR 0");
        Assert.assertTrue(expr instanceof LiteralExpression);
        Assert.assertEquals("true", expr.serializeToText(false));
    }

    @Test
    public void testHasTokenReplacer_NotReplaced() {
        IExpression expr = ExpressionASTBuilder.build("hasToken(a, 'ab')");

        Assert.assertEquals("hasToken(a, 'ab')", expr.serializeToText(false));
    }

    @Test
    public void testHasTokenReplacer_Replaced() {
        IExpression expr = ExpressionASTBuilder.build("hasToken(a, 'a_b')");

        Assert.assertEquals("a like '%a_b%'", expr.serializeToText(false));
    }
}
