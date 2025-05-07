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

package org.bithon.server.datasource.expression;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Frank Chen
 * @date 30/8/23 9:40 am
 */
public class FunctionsTest {

    private final ExpressionASTBuilder builder = ExpressionASTBuilder.builder().functions(Functions.getInstance());

    @Test
    public void test_round() {
        Assertions.assertEquals("5.00",
                                builder.build("round(5, 2)").serializeToText());

        Assertions.assertEquals("100.000",
                                builder.build("round(100,3)").serializeToText());
    }

    @Test
    public void test_round_InvalidParameterCount() {
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("round(100,99,98)"));
    }

    @Test
    public void test_round_ExpressionAsParameter() {
        String expression = "round(a*b/c+d,2)";
        Assertions.assertEquals("round(((a * b) / c) + d, 2)", builder.build(expression).serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_startsWith() {
        Assertions.assertEquals(true,
                                builder.build("startsWith('bithon', 'b')").evaluate(null));

        Assertions.assertEquals(true,
                                builder.build("startsWith('bithon', '')").evaluate(null));

        Assertions.assertEquals(false,
                                builder.build("startsWith('bithon', 'x')").evaluate(null));

        // Check if the given is null
        Assertions.assertEquals(false,
                                builder.build("startsWith(a, 'x')").evaluate(name -> null));

        Assertions.assertEquals(false,
                                builder.build("startsWith('bithon', a)").evaluate(name -> null));

        // Check if the givens are all variables
        Assertions.assertEquals(true,
                                builder.build("startsWith(a, b)").evaluate(name -> {
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
        Assertions.assertEquals(true,
                                builder.build("endsWith('bithon', 'on')").evaluate(null));

        Assertions.assertEquals(true,
                                builder.build("endsWith('bithon', '')").evaluate(null));

        Assertions.assertEquals(false,
                                builder.build("endsWith('bithon', 'x')").evaluate(null));

        // Check if the given is null
        Assertions.assertEquals(false,
                                builder.build("endsWith(a, 'x')").evaluate(name -> null));

        Assertions.assertEquals(false,
                                builder.build("endsWith('bithon', a)").evaluate(name -> null));

        // Check if the givens are all variables
        Assertions.assertEquals(true,
                                builder.build("endsWith(a, b)").evaluate(name -> {
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
        Assertions.assertEquals(true,
                                builder.build("hasToken('bithon', 'th')").evaluate(null));

        Assertions.assertEquals(true,
                                builder.build("hasToken('bithon', '')").evaluate(null));

        Assertions.assertEquals(false,
                                builder.build("hasToken('bithon', 'x')").evaluate(null));

        // Check if the given is null
        Assertions.assertEquals(false,
                                builder.build("hasToken(a, 'x')").evaluate(name -> null));

        // The 2nd parameter must be a constant
        Assertions.assertThrows(InvalidExpressionException.class,
                                () -> builder.build("hasToken('bithon', a)").evaluate(name -> null));

        // The 2nd parameter must be type of string
        Assertions.assertThrows(InvalidExpressionException.class,
                                () -> builder.build("hasToken('bithon', 1)").evaluate(name -> null));

        // Check if the givens are all variables
        Assertions.assertEquals(true,
                                builder.build("hasToken(a, 'b')").evaluate(name -> {
                                    if ("a".equals(name)) {
                                        return "bithon";
                                    }
                                    return null;
                                }));
    }

    @Test
    public void test_lower() {
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("lower()"));

        Assertions.assertEquals("bithon",
                                builder.build("lower('bithon')").evaluate(null));

        Assertions.assertEquals("bithon",
                                builder.build("lower('Bithon')").evaluate(null));

        Assertions.assertEquals("bithon",
                                builder.build("lower('BITHON')").evaluate(null));

        // Check if the given is null
        Assertions.assertNull(builder.build("lower(a)").evaluate(name -> null));
    }

    @Test
    public void test_upper() {
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("upper()"));

        Assertions.assertEquals("BITHON",
                                builder.build("upper('bithon')").evaluate(null));

        Assertions.assertEquals("BITHON",
                                builder.build("upper('BITHON')").evaluate(null));

        Assertions.assertEquals("BITHON",
                                builder.build("upper('BithON')").evaluate(null));

        // Check if the given is null
        Assertions.assertNull(builder.build("upper(a)").evaluate(name -> null));
    }

    @Test
    public void test_substring() {
        // invalid parameter number
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("substring()"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("substring(a)"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("substring(a,b)"));

        //
        // invalid parameter type
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("substring(1,2,3)"));
        // 2nd or 3rd should be integer
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("substring('a','a',3)"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("substring('a','a','a')"));

        Assertions.assertEquals("bi",
                                builder.build("substring('bithon', 0, 2)").evaluate(null));

        Assertions.assertEquals("it",
                                builder.build("substring('bithon', 1, 2)").evaluate(null));

        // Check if the given is null
        Assertions.assertNull(builder.build("substring(a, 1, 2)").evaluate(name -> null));
    }

    @Test
    public void test_trim() {
        // invalid parameter number
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("trim()"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("trim(a, b)"));

        //
        // invalid parameter type
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("trim(1)"));

        Assertions.assertEquals("a",
                                builder.build("trim(' a ')").evaluate(null));

        // Check if the given is null
        Assertions.assertNull(builder.build("trim(a)").evaluate(name -> null));
    }

    @Test
    public void test_trimLeft() {
        // invalid parameter number
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("trimLeft()"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("trimLeft(a, b)"));

        //
        // invalid parameter type
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("trimLeft(1)"));

        Assertions.assertEquals("a ",
                                builder.build("trimLeft(' a ')").evaluate(null));

        // Check if the given is null
        Assertions.assertNull(builder.build("trimLeft(a)").evaluate(name -> null));
    }

    @Test
    public void test_trimRight() {
        // invalid parameter number
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("trimRight()"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("trimRight(a, b)"));

        //
        // invalid parameter type
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("trimRight(1)"));

        Assertions.assertEquals(" a",
                                builder.build("trimRight(' a ')").evaluate(null));

        // Check if the given is null
        Assertions.assertNull(builder.build("trimRight(a)").evaluate(name -> null));
    }

    @Test
    public void test_length() {
        // invalid parameter number
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("length()"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("length(a, b)"));

        //
        // invalid parameter type
        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("length(1)"));

        Assertions.assertEquals(" a",
                                builder.build("trimRight(' a ')").evaluate(null));

        // Check if the given is null
        Assertions.assertNull(builder.build("trimRight(a)").evaluate(name -> null));
    }

    @Test
    public void test_count() {
        IExpression expr = ExpressionASTBuilder.builder().functions(Functions.getInstance()).build("count(1)");

        Assertions.assertEquals("count(1)", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_toMilliseconds() {
        Assertions.assertEquals(1000L,
                                builder.build("toMilliseconds(1)").evaluate(null));

        Assertions.assertEquals(2000L,
                                builder.build("toMilliseconds(2)").evaluate(null));
    }

    @Test
    public void test_toMicroseconds() {
        Assertions.assertEquals(1000000L,
                                builder.build("toMicroseconds(1)").evaluate(null));

        Assertions.assertEquals(2000000L,
                                builder.build("toMicroseconds(2s)").evaluate(null));
    }

    @Test
    public void test_toNanoseconds() {
        Assertions.assertEquals(1000000000L,
                                builder.build("toNanoseconds(1)").evaluate(null));

        Assertions.assertEquals(2000000000L,
                                builder.build("toNanoseconds(2)").evaluate(null));
    }

    @Test
    public void test_concat() {
        Assertions.assertEquals("hello world",
                                builder.build("concat('hello',' world')").evaluate(null));

        Assertions.assertEquals("hello ",
                                builder.build("concat('hello',' ')").evaluate(null));

        Assertions.assertThrows(InvalidExpressionException.class, () -> builder.build("concat('hello',1"));
    }
}
