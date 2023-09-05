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

import org.junit.Assert;
import org.junit.Test;

public class FieldExpressionTest {

    @Test
    public void testVariableExpression() {
        String expression = "1000/{interval}";
        Assert.assertEquals("1000 / {interval}", ExpressionASTBuilder.build(expression).serializeToText());
    }

    /**
     * Make sure the lexer and parser won't treat the expression as function expression
     */
    @Test
    public void testParenthesesAroundExpression() {
        String expression = "({interval}*100)";
        Assert.assertEquals("{interval} * 100", ExpressionASTBuilder.build(expression).serializeToText());
    }

    @Test
    public void testFunctionExpression() {
        String expression = "round(100,3)";
        Assert.assertEquals("100.000", ExpressionASTBuilder.build(expression).serializeToText());
    }

    @Test
    public void testExpressionInFunctionExpression() {
        String expression = "round(a*b/c+d,2)";
        Assert.assertEquals("round(((a * b) / c) + d,2)", ExpressionASTBuilder.build(expression).serializeToText(false));
    }

    @Test
    public void testMoreArgumentInFunctionExpression() {
        String functionExpression = "round(100,99,98)";

        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build(functionExpression));
    }
}
