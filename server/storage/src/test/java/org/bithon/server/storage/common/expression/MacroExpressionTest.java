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

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.junit.Assert;
import org.junit.Test;

public class MacroExpressionTest {

    @Test
    public void testVariableExpression() {
        String expression = "1000/{interval}";
        Assert.assertEquals("1000 / {interval}", ExpressionASTBuilder.builder().build(expression).serializeToText());
    }

    /**
     * Make sure the lexer and parser won't treat the expression as function expression
     */
    @Test
    public void testParenthesesAroundExpression() {
        String expression = "({interval}*100)";
        Assert.assertEquals("{interval} * 100", ExpressionASTBuilder.builder().build(expression).serializeToText());
    }

    @Test
    public void testMacroExpression() {
        IExpression expr = ExpressionASTBuilder.builder().build("{a}");
        Assert.assertTrue(expr instanceof MacroExpression);
        Assert.assertEquals("a", ((MacroExpression) expr).getMacro());

        Assert.assertEquals("1", expr.evaluate(name -> "a".equals(name) ? "1" : null));
    }
}
