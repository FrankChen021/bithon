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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/3/14 22:00
 */
public class LikeExpressionTest {

    @Test
    public void test1() {
        // a LIKE '%bison%'
        ConditionalExpression.Like expr = new ConditionalExpression.Like(new IdentifierExpression("a"),
                                                                         new LiteralExpression.StringLiteral("%an%"));
        Assert.assertTrue(expr.evaluate(name -> "bison is an animal"));
    }

    @Test
    public void test2() {
        // a LIKE 'bison%'
        ConditionalExpression.Like expr = new ConditionalExpression.Like(new IdentifierExpression("a"),
                                                                         new LiteralExpression.StringLiteral("bison%"));
        Assert.assertTrue(expr.evaluate(name -> "bison is an animal"));
        Assert.assertTrue(expr.evaluate(name -> "bison"));
    }

    @Test
    public void test3() {
        // a LIKE 'bison'
        ConditionalExpression.Like expr = new ConditionalExpression.Like(new IdentifierExpression("a"),
                                                                         new LiteralExpression.StringLiteral("bison"));
        Assert.assertTrue(expr.evaluate(name -> "bison"));
        Assert.assertFalse(expr.evaluate(name -> "bisonn"));
    }

    @Test
    public void test4() {
        // a LIKE 'bison'
        ConditionalExpression.Like expr = new ConditionalExpression.Like(new IdentifierExpression("a"),
                                                                         new LiteralExpression.StringLiteral("%bison"));
        Assert.assertTrue(expr.evaluate(name -> "american bison"));
        Assert.assertTrue(expr.evaluate(name -> "bison"));
    }
}
