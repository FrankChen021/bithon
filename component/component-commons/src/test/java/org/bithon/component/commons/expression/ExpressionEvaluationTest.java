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

package org.bithon.component.commons.expression;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/3/14 22:00
 */
public class ExpressionEvaluationTest {

    @Test
    public void test_ContainsExpression() {
        // a contains 'bison'
        IExpression expr = new ConditionalExpression.Contains(new IdentifierExpression("a"),
                                                              new LiteralExpression.StringLiteral("bison"));
        Assertions.assertTrue((boolean) expr.evaluate(name -> "american bison"));
        Assertions.assertTrue((boolean) expr.evaluate(name -> "bison"));
        Assertions.assertTrue((boolean) expr.evaluate(name -> "bison_is_an_animal"));
        Assertions.assertTrue((boolean) expr.evaluate(name -> "one_kind of animal_is_bison_"));
    }

    @Test
    public void test_StartsWithExpression() {
        // a startsWith 'bison'
        IExpression expr = new ConditionalExpression.StartsWith(new IdentifierExpression("a"),
                                                                new LiteralExpression.StringLiteral("bison"));
        Assertions.assertTrue((boolean) expr.evaluate(name -> "bison is an animal"));
        Assertions.assertFalse((boolean) expr.evaluate(name -> "i don't think bison is an animal"));
        Assertions.assertFalse((boolean) expr.evaluate(name -> "one kind of animal is bison"));
    }

    @Test
    public void test_EndsWithExpression() {
        // a endsWith 'bison'
        IExpression expr = new ConditionalExpression.EndsWith(new IdentifierExpression("a"),
                                                              new LiteralExpression.StringLiteral("bison"));
        Assertions.assertFalse((boolean) expr.evaluate(name -> "bison is an animal"));
        Assertions.assertFalse((boolean) expr.evaluate(name -> "i don't think bison is an animal"));
        Assertions.assertTrue((boolean) expr.evaluate(name -> "one kind of animal is bison"));
    }
}
