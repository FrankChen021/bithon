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

package org.bithon.agent.instrumentation.aop.interceptor;

import org.bithon.agent.instrumentation.aop.interceptor.expression.parser.ExpressionParser;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/2/7 11:25
 */
public class InterceptorExpressionTest {


    @Test
    public void testArgsExpressionSyntax() {
        ExpressionParser.createGrammarParser("args. length")
                        .objectExpression();

        ExpressionParser.createGrammarParser("args.size")
                        .objectExpression();

        ExpressionParser.createGrammarParser("args [ 1 ]")
                        .objectExpression();

        ExpressionParser.createGrammarParser("args[1]")
                        .objectExpression();

        ExpressionParser.createGrammarParser("args[1].type")
                        .objectExpression();
    }

    @Test
    public void testWhenExpressionSyntax() {
        ExpressionParser.createGrammarParser("when has() ")
                        .whenExpression();

        ExpressionParser.createGrammarParser("when has('a') ")
                        .whenExpression();

        ExpressionParser.createGrammarParser("when has('a', 'b', 3)")
                        .whenExpression();
    }

    @Test
    public void testClassExpressionSyntax() {
        ExpressionParser.createGrammarParser("name('InterceptorExpressionTest')")
                        .classExpression();

        ExpressionParser.createGrammarParser("in('org.bithon.InterceptorExpressionTest', '2')")
                        .classExpression();

        ExpressionParser.createGrammarParser("org.bithon.InterceptorExpressionTest")
                        .classExpression();
    }

    @Test
    public void testMethodExpressionSyntax() {
        ExpressionParser.createGrammarParser("print()")
                        .methodExpression();

        ExpressionParser.createGrammarParser("print(6)")
                        .methodExpression();

        ExpressionParser.createGrammarParser("print(args[0] = 'a')")
                        .methodExpression();

        ExpressionParser.createGrammarParser("print(args[0] = 'int' and args[4] = 'String')")
                        .methodExpression();
    }

    @Test
    public void testParseExpression() {
        ExpressionParser.parse("org.bithon.InterceptorExpressionTest#print()");

        ExpressionParser.parse("in('org.bithon.InterceptorExpressionTest')#print()");

        ExpressionParser.parse("in('org.bithon.InterceptorExpressionTest', 'org.bithon.InterceptorExpressionTest2')#print(1)");

        ExpressionParser.parse("when has('org.apache.http.client5.HttpClient') public org.bithon.InterceptorExpressionTest#print1(args.length=4 and args[0] ='String' )");
    }
}
