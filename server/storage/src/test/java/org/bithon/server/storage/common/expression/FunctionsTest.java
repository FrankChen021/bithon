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

import java.math.BigDecimal;

/**
 * @author Frank Chen
 * @date 30/8/23 9:40 am
 */
public class FunctionsTest {
    @Test
    public void testRound() {
        Assert.assertEquals(BigDecimal.valueOf(5.0).setScale(2),
                            ExpressionASTBuilder.build("round(5, 2)").evaluate(null));
    }

    @Test
    public void test_startsWith() {
        Assert.assertEquals(true,
                            ExpressionASTBuilder.build("startsWith('bithon', 'b')").evaluate(null));

        Assert.assertEquals(true,
                            ExpressionASTBuilder.build("startsWith('bithon', '')").evaluate(null));

        Assert.assertEquals(false,
                            ExpressionASTBuilder.build("startsWith('bithon', 'x')").evaluate(null));

        // Check if it works if the given is null
        Assert.assertEquals(false,
                            ExpressionASTBuilder.build("startsWith(a, 'x')").evaluate(name -> null));

        Assert.assertEquals(false,
                            ExpressionASTBuilder.build("startsWith('bithon', a)").evaluate(name -> null));

        // Check if it works if the given are all variables
        Assert.assertEquals(true,
                            ExpressionASTBuilder.build("startsWith(a, b)").evaluate(name -> {
                                if ("a".equals(name)) {
                                    return "bithon";
                                }
                                if ("b".equals(name)) {
                                    return "b";
                                }
                                return null;
                            }));
    }

    @Test
    public void test_endsWith() {
        Assert.assertEquals(true,
                            ExpressionASTBuilder.build("endsWith('bithon', 'on')").evaluate(null));

        Assert.assertEquals(true,
                            ExpressionASTBuilder.build("endsWith('bithon', '')").evaluate(null));

        Assert.assertEquals(false,
                            ExpressionASTBuilder.build("endsWith('bithon', 'x')").evaluate(null));

        // Check if it works if the given is null
        Assert.assertEquals(false,
                            ExpressionASTBuilder.build("endsWith(a, 'x')").evaluate(name -> null));

        Assert.assertEquals(false,
                            ExpressionASTBuilder.build("endsWith('bithon', a)").evaluate(name -> null));

        // Check if it works if the given are all variables
        Assert.assertEquals(true,
                            ExpressionASTBuilder.build("endsWith(a, b)").evaluate(name -> {
                                if ("a".equals(name)) {
                                    return "bithon";
                                } else if ("b".equals(name)) {
                                    return "on";
                                } else {
                                    return null;
                                }
                            }));
    }

    @Test
    public void test_hasToken() {
        Assert.assertEquals(true,
                            ExpressionASTBuilder.build("hasToken('bithon', 'th')").evaluate(null));

        Assert.assertEquals(true,
                            ExpressionASTBuilder.build("hasToken('bithon', '')").evaluate(null));

        Assert.assertEquals(false,
                            ExpressionASTBuilder.build("hasToken('bithon', 'x')").evaluate(null));

        // Check if it works if the given is null
        Assert.assertEquals(false,
                            ExpressionASTBuilder.build("hasToken(a, 'x')").evaluate(name -> null));

        // The 2nd parameter must be a constant
        Assert.assertThrows(InvalidExpressionException.class,
                            () -> ExpressionASTBuilder.build("hasToken('bithon', a)").evaluate(name -> null));

        // The 2nd parameter must be type of string
        Assert.assertThrows(InvalidExpressionException.class,
                            () -> ExpressionASTBuilder.build("hasToken('bithon', 1)").evaluate(name -> null));

        // Check if it works if the given are all variables
        Assert.assertEquals(true,
                            ExpressionASTBuilder.build("hasToken(a, 'b')").evaluate(name -> {
                                if ("a".equals(name)) {
                                    return "bithon";
                                }
                                return null;
                            }));
    }

    @Test
    public void test_lower() {
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("lower()"));

        Assert.assertEquals("bithon",
                            ExpressionASTBuilder.build("lower('bithon')").evaluate(null));

        Assert.assertEquals("bithon",
                            ExpressionASTBuilder.build("lower('Bithon')").evaluate(null));

        Assert.assertEquals("bithon",
                            ExpressionASTBuilder.build("lower('BITHON')").evaluate(null));

        // Check if it works if the given is null
        Assert.assertNull(ExpressionASTBuilder.build("lower(a)").evaluate(name -> null));
    }

    @Test
    public void test_upper() {
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("upper()"));

        Assert.assertEquals("BITHON",
                            ExpressionASTBuilder.build("upper('bithon')").evaluate(null));

        Assert.assertEquals("BITHON",
                            ExpressionASTBuilder.build("upper('BITHON')").evaluate(null));

        Assert.assertEquals("BITHON",
                            ExpressionASTBuilder.build("upper('BithON')").evaluate(null));

        // Check if it works if the given is null
        Assert.assertNull(ExpressionASTBuilder.build("upper(a)").evaluate(name -> null));
    }

    @Test
    public void test_substring() {
        // invalid parameter number
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("substring()"));
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("substring(a)"));
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("substring(a,b)"));

        //
        // invalid parameter type
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("substring(1,2,3)"));
        // 2nd or 3rd should be integer
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("substring('a','a',3)"));
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("substring('a','a','a')"));

        Assert.assertEquals("bi",
                            ExpressionASTBuilder.build("substring('bithon', 0, 2)").evaluate(null));

        Assert.assertEquals("it",
                            ExpressionASTBuilder.build("substring('bithon', 1, 2)").evaluate(null));

        // Check if it works if the given is null
        Assert.assertNull(ExpressionASTBuilder.build("substring(a, 1, 2)").evaluate(name -> null));
    }

    @Test
    public void test_trim() {
        // invalid parameter number
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("trim()"));
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("trim(a, b)"));

        //
        // invalid parameter type
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("trim(1)"));

        Assert.assertEquals("a",
                            ExpressionASTBuilder.build("trim(' a ')").evaluate(null));

        // Check if it works if the given is null
        Assert.assertNull(ExpressionASTBuilder.build("trim(a)").evaluate(name -> null));
    }

    @Test
    public void test_trimLeft() {
        // invalid parameter number
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("trimLeft()"));
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("trimLeft(a, b)"));

        //
        // invalid parameter type
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("trimLeft(1)"));

        Assert.assertEquals("a ",
                            ExpressionASTBuilder.build("trimLeft(' a ')").evaluate(null));

        // Check if it works if the given is null
        Assert.assertNull(ExpressionASTBuilder.build("trimLeft(a)").evaluate(name -> null));
    }

    @Test
    public void test_trimRight() {
        // invalid parameter number
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("trimRight()"));
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("trimRight(a, b)"));

        //
        // invalid parameter type
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("trimRight(1)"));

        Assert.assertEquals(" a",
                            ExpressionASTBuilder.build("trimRight(' a ')").evaluate(null));

        // Check if it works if the given is null
        Assert.assertNull(ExpressionASTBuilder.build("trimRight(a)").evaluate(name -> null));
    }

    @Test
    public void test_length() {
        // invalid parameter number
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("length()"));
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("length(a, b)"));

        //
        // invalid parameter type
        Assert.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.build("length(1)"));

        Assert.assertEquals(" a",
                            ExpressionASTBuilder.build("trimRight(' a ')").evaluate(null));

        // Check if it works if the given is null
        Assert.assertNull(ExpressionASTBuilder.build("trimRight(a)").evaluate(name -> null));
    }
}
