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

import org.bithon.agent.instrumentation.aop.interceptor.expression.ExpressionParser;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/2/7 11:25
 */
public class InterceptorExpressionTest {


    @Test
    public void testArgsSyntax() {
        ExpressionParser.create("args. length")
                        .methodObjectExpression();

        ExpressionParser.create("args.size")
                        .methodObjectExpression();

        ExpressionParser.create("args [ 1 ]")
                        .methodObjectExpression();

        ExpressionParser.create("args[1]")
                        .methodObjectExpression();

        ExpressionParser.create("args[1].type")
                        .methodObjectExpression();
    }

    @Test
    public void testWhenExpression() {
        ExpressionParser.create("when has() ")
                        .whenExpression();

        ExpressionParser.create("when has('a') ")
                        .whenExpression();

        ExpressionParser.create("when has('a', 'b', 3)")
                        .whenExpression();
    }

    @Test
    public void testClassSyntax() {
        ExpressionParser.create("FOR class.fn('InterceptorExpressionTest')")
                        .classFilterExpression();

        ExpressionParser.create("for class.in('org.bithon.InterceptorExpressionTest', '2')")
                        .classFilterExpression();
    }

    @Test
    public void testExpression() {
        ExpressionParser.create("for class.name('org.bithon.InterceptorExpressionTest') on method = 'print' ")
                        .parse();

        ExpressionParser.create("for class.name('org.bithon.InterceptorExpressionTest') "
                                + "on (method = 'print')"
                                + "AND (args.length = 1)")
                        .parse();

        ExpressionParser.create("for class.name('org.bithon.InterceptorExpressionTest') "
                                + "\non ("
                                + " (method = 'print1' AND method = 'm2')"
                                + ")")
                        .parse();

        ExpressionParser.create("for class.name('org.bithon.InterceptorExpressionTest') "
                                + "\n ON ("
                                + " (method = 'print1' or method = 'm2')"
                                + "\nOR (method = 'print2' AND args.length = 4)"
                                + "\nOR (method = 'print3' AND args.length = 4 AND args[0].type = 3)"
                                + "\nOR (method = 'print4' AND args.length = 4)"
                                + ")")
                        .parse();
    }
}
