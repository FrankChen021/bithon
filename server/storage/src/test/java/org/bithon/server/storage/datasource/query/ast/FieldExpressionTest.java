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

package org.bithon.server.storage.datasource.query.ast;

import org.bithon.server.storage.datasource.query.parser.FieldExpressionParserImpl;
import org.bithon.server.storage.datasource.query.parser.FieldExpressionVisitorAdaptor;
import org.junit.Assert;
import org.junit.Test;

public class FieldExpressionTest {

    @Test
    public void testVariableExpression() {
        String expression = "1000/{interval}";

        ExpressionGenerator g = new ExpressionGenerator();
        FieldExpressionParserImpl.create(expression).visit(g);
        Assert.assertEquals("1000/{interval}", g.getGenerated());
    }

    /**
     * Make sure the lexer and parser won't treat the expression as function expression
     */
    @Test
    public void testParenthesesAroundExpression() {
        String expression = "({interval}*100)";

        ExpressionGenerator g = new ExpressionGenerator();
        FieldExpressionParserImpl.create(expression).visit(g);
        Assert.assertEquals("({interval}*100)", g.getGenerated());
    }

    @Test
    public void testFunctionExpression() {
        String expression = "round(100,2)";

        ExpressionGenerator g = new ExpressionGenerator();
        FieldExpressionParserImpl.create(expression).visit(g);
        Assert.assertEquals(expression, g.getGenerated());
    }

    @Test
    public void testExpressionInFunctionExpression() {
        String expression = "round(a*b/c+d,2)";

        ExpressionGenerator g = new ExpressionGenerator();
        FieldExpressionParserImpl.create(expression).visit(g);
        Assert.assertEquals(expression, g.getGenerated());
    }

    @Test
    public void testMoreArgumentInFunctionExpression() {
        String functionExpression = "round(100,99,98)";


        try {
            ExpressionGenerator g = new ExpressionGenerator();
            FieldExpressionParserImpl.create(functionExpression).visit(g);

            // Should never go here
            Assert.fail();
        } catch (IllegalStateException e) {
            Assert.assertEquals("In expression [round(100,99,98)], function [round] has [2] parameters, but only given [3]", e.getMessage());
        }
    }

    private static class ExpressionGenerator implements FieldExpressionVisitorAdaptor {
        private final StringBuilder sb = new StringBuilder(64);

        @Override
        public void visitConstant(String number) {
            sb.append(number);
        }

        @Override
        public void visitorOperator(String operator) {
            sb.append(operator);
        }

        @Override
        public void visitVariable(String variable) {
            sb.append('{');
            sb.append(variable);
            sb.append('}');
        }

        @Override
        public void beginFunction(String name) {
            sb.append(name);
            sb.append('(');
        }

        @Override
        public void endFunction() {
            sb.append(')');
        }

        @Override
        public void endFunctionArgument(int argIndex, int count) {
            if (argIndex < count - 1) {
                sb.append(',');
            }
        }

        @Override
        public void beginSubExpression() {
            sb.append('(');
        }

        @Override
        public void endSubExpression() {
            sb.append(')');
        }

        @Override
        public void visitField(String field) {
            sb.append(field);
        }

        public String getGenerated() {
            return sb.toString();
        }
    }
}
