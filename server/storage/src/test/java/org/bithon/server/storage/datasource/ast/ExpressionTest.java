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

package org.bithon.server.storage.datasource.ast;

import org.bithon.component.commons.expression.IExpression;
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
 * @author frank.chen021@outlook.com
 * @date 2023/8/19 17:23
 */
public class ExpressionTest {

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
                public void validateParameter(int index, Object parameter) {
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
    public void testFunction() {
        IExpression expr = ExpressionASTBuilder.build("sum(1,2)", new FunctionProvider());
        Assert.assertEquals(3L, expr.evaluate(null));
    }

    @Test
    public void testFunction_Nested() {
        IExpression expr = ExpressionASTBuilder.build("sum(1,sum(2,3))", new FunctionProvider());
        Assert.assertEquals(6L, expr.evaluate(null));
    }

    @Test
    public void testFunction_Mixed() {
        IExpression expr = ExpressionASTBuilder.build("1 + sum(1,sum(2,3))", new FunctionProvider());
        Assert.assertEquals(7L, expr.evaluate(null));
    }

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
}
