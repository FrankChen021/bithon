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

package org.bithon.server.datasource.reader.clickhouse.expression;

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.bithon.server.datasource.reader.jdbc.dialect.LikeOperator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * @author frank.chen021@outlook.com
 * @date 14/5/25 2:00 pm
 */
public class RegularExpressionMatchOptimizerTest {

    private final IExpression testColumn = new IdentifierExpression("column");

    @Test
    public void testEmptyPattern() {
        IExpression expr = createMatchExpression("");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.EQ.class, optimized);
        assertEquals("column = ''", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testExactMatch() {
        IExpression expr = createMatchExpression("^abc$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.EQ.class, optimized);
        assertEquals("column = 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testStartsWith() {
        IExpression expr = createMatchExpression("^abc");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.StartsWith.class, optimized);
        assertEquals("column startsWith 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testStartsWithStar() {
        IExpression expr = createMatchExpression("^abc.*");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.StartsWith.class, optimized);
        assertEquals("column startsWith 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testEndsWith() {
        IExpression expr = createMatchExpression("abc$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.EndsWith.class, optimized);
        assertEquals("column endsWith 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testEndsWithStar() {
        IExpression expr = createMatchExpression(".*abc$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.EndsWith.class, optimized);
        assertEquals("column endsWith 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testContains() {
        IExpression expr = createMatchExpression("abc");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.Contains.class, optimized);
        assertEquals("column contains 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testContainsWithStars() {
        IExpression expr = createMatchExpression(".*abc.*");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.Contains.class, optimized);
        assertEquals("column contains 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testInOperator() {
        IExpression expr = createMatchExpression("^(a|b|c)$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.In.class, optimized);
        assertEquals("column in ('a', 'b', 'c')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testInOperatorWithoutAnchors() {
        IExpression expr = createMatchExpression("(a|b|c)");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.In.class, optimized);
        assertEquals("column in ('a', 'b', 'c')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testEscapedMetacharacters() {
        // Pattern with escaped metacharacters should not be optimized
        IExpression expr = createMatchExpression("a\\.b");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        // Should be optimized to Contains since . is escaped
        assertInstanceOf(ConditionalExpression.Contains.class, optimized);
        assertEquals("column contains 'a\\.b'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testUnescapedMetacharacters() {
        // Pattern with unescaped metacharacters should not be optimized
        IExpression expr = createMatchExpression("a?b");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        // Should remain as RegexMatch
        assertInstanceOf(ConditionalExpression.RegularExpressionMatchExpression.class, optimized);
    }

    @Test
    public void testComplexPattern() {
        // A complex pattern that shouldn't be optimized
        IExpression expr = createMatchExpression("^a(b|c)*d+e?");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.RegularExpressionMatchExpression.class, optimized);
    }

    @Test
    public void testNotMatch_ExactMatch() {
        IExpression expr = createNotMatchExpression("^abc$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.NE.class, optimized);
        assertEquals("column <> 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNotMatch_StartsWith() {
        IExpression expr = createNotMatchExpression("^abc");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(LogicalExpression.NOT.class, optimized);
        assertEquals("NOT (column startsWith 'abc')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNotMatch_EndsWith() {
        IExpression expr = createNotMatchExpression("abc$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(LogicalExpression.NOT.class, optimized);
        assertEquals("NOT (column endsWith 'abc')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNotMatch_Contains() {
        IExpression expr = createNotMatchExpression("abc");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(LogicalExpression.NOT.class, optimized);
        assertEquals("NOT (column contains 'abc')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNotMatch_In() {
        IExpression expr = createNotMatchExpression("^(a|b|c)$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.NotIn.class, optimized);
        assertEquals("column not in ('a', 'b', 'c')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testSingleDotPattern() {
        IExpression expr = createMatchExpression("a.b");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like '%a_b%'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testSingleDotPatternWithAnchors() {
        IExpression expr = createMatchExpression("^a.b$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like 'a_b'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testSingleDotPatternWithStartAnchor() {
        IExpression expr = createMatchExpression("^a.b");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like 'a_b%'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testSingleDotPatternWithEndAnchor() {
        IExpression expr = createMatchExpression("a.b$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like '%a_b'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testMultipleDotPattern() {
        IExpression expr = createMatchExpression("a.b.c");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like '%a_b_c%'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testConsecutiveDotPattern() {
        IExpression expr = createMatchExpression("a..b");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like '%a__b%'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNotMatch_Like() {
        IExpression expr = createNotMatchExpression("a.b");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(LogicalExpression.NOT.class, optimized);
        assertEquals("NOT (column like '%a_b%')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testEscapedDotPattern() {
        IExpression expr = createMatchExpression("a\\.b");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        // Escaped dot should not be converted to underscore
        assertInstanceOf(ConditionalExpression.Contains.class, optimized);
        assertEquals("column contains 'a\\.b'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testMixedDotAndEscapedDot() {
        IExpression expr = createMatchExpression("a.b\\.c");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like '%a_b.c%'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testDigitsOnlyPattern() {
        IExpression expr = createMatchExpression("^[0-9]+$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.GT.class, optimized);
        assertEquals("column > '0'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testDigitsOnlyPatternWithOptional() {
        IExpression expr = createMatchExpression("^[0-9]*$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.GTE.class, optimized);
        assertEquals("column >= '0'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testDigitsShorthandPattern() {
        IExpression expr = createMatchExpression("^\\d+$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.GT.class, optimized);
        assertEquals("column > '0'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testWordCharPattern() {
        IExpression expr = createMatchExpression("^\\w+$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like '[a-zA-Z0-9_]%'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNotMatch_DigitsOnly() {
        IExpression expr = createNotMatchExpression("^[0-9]+$");
        IExpression optimized = RegularExpressionMatchOptimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.LTE.class, optimized);
        assertEquals("column <= '0'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testEmptyRHS() {
        // Test with non-string literal right-hand side
        ConditionalExpression.RegularExpressionMatchExpression expr = new ConditionalExpression.RegularExpressionMatchExpression(
            testColumn, 
            new IdentifierExpression("pattern") // Not a string literal
        );
        IExpression optimized = RegularExpressionMatchOptimizer.optimize(expr);

        // Should remain unchanged
        assertInstanceOf(ConditionalExpression.RegularExpressionMatchExpression.class, optimized);
    }

    private IExpression createMatchExpression(String pattern) {
        return new ConditionalExpression.RegularExpressionMatchExpression(
            testColumn,
            new LiteralExpression.StringLiteral(pattern)
        );
    }

    private IExpression createNotMatchExpression(String pattern) {
        return new ConditionalExpression.RegularExpressionNotMatchExpression(
            testColumn,
            new LiteralExpression.StringLiteral(pattern)
        );
    }
}