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

package org.bithon.server.datasource.reader.jdbc.dialect;

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * @author frank.chen021@outlook.com
 * @date 14/5/25 2:00 pm
 */
public class RegularExpressionMatchOptimizerTest {

    private final IExpression testColumn = new IdentifierExpression("column");
    private final RegularExpressionMatchOptimizer optimizer = RegularExpressionMatchOptimizer.of(QuerySettings.builder()
                                                                                                              .enableRegularExpressionOptimization(true)
                                                                                                              .enableRegularExpressionOptimizationToStartsWith(true)
                                                                                                              .enableRegularExpressionOptimizationToEndsWith(true)
                                                                                                              .build());

    @Test
    public void testEmptyPattern() {
        IExpression expr = createMatchExpression("");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.EQ.class, optimized);
        assertEquals("column = ''", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testExactMatch() {
        IExpression expr = createMatchExpression("^abc$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.EQ.class, optimized);
        assertEquals("column = 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testStartsWith() {
        IExpression expr = createMatchExpression("^abc");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.StartsWith.class, optimized);
        assertEquals("column startsWith 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testStartsWithStar() {
        IExpression expr = createMatchExpression("^abc.*");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.StartsWith.class, optimized);
        assertEquals("column startsWith 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testDisabledStartsWithOptimization() {
        // Create an optimizer with startsWith optimization disabled
        RegularExpressionMatchOptimizer disabledStartsWithOptimizer = RegularExpressionMatchOptimizer.of(
            QuerySettings.builder()
                         .enableRegularExpressionOptimization(true)
                         .enableRegularExpressionOptimizationToStartsWith(false)
                         .build()
        );
        
        IExpression expr = createMatchExpression("^abc");
        IExpression optimized = disabledStartsWithOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);
        
        // Should not be optimized to startsWith
        assertInstanceOf(ConditionalExpression.RegularExpressionMatchExpression.class, optimized);
        assertEquals("column =~ '^abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
        
        // Also test with "^prefix.*" pattern
        expr = createMatchExpression("^bithon.*");
        optimized = disabledStartsWithOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);
        
        // Should not be optimized to startsWith
        assertInstanceOf(ConditionalExpression.RegularExpressionMatchExpression.class, optimized);
        assertEquals("column =~ '^bithon.*'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testEndsWith() {
        IExpression expr = createMatchExpression("abc$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.EndsWith.class, optimized);
        assertEquals("column endsWith 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testEndsWithStar() {
        IExpression expr = createMatchExpression(".*abc$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.EndsWith.class, optimized);
        assertEquals("column endsWith 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testDisabledEndsWithOptimization() {
        // Create an optimizer with endsWith optimization disabled
        RegularExpressionMatchOptimizer disabledEndsWithOptimizer = RegularExpressionMatchOptimizer.of(
            QuerySettings.builder()
                         .enableRegularExpressionOptimization(true)
                         .enableRegularExpressionOptimizationToEndsWith(false)
                         .build()
        );
        
        IExpression expr = createMatchExpression("abc$");
        IExpression optimized = disabledEndsWithOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);
        
        // Should not be optimized to endsWith
        assertInstanceOf(ConditionalExpression.RegularExpressionMatchExpression.class, optimized);
        assertEquals("column =~ 'abc$'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
        
        // Also test with ".*suffix$" pattern
        expr = createMatchExpression(".*org$");
        optimized = disabledEndsWithOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);
        
        // Should not be optimized to endsWith
        assertInstanceOf(ConditionalExpression.RegularExpressionMatchExpression.class, optimized);
        assertEquals("column =~ '.*org$'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }
    
    @Test
    public void testBithonPattern() {
        // Test the specific pattern mentioned by the user
        IExpression expr = createMatchExpression("^bithon.*");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.StartsWith.class, optimized);
        assertEquals("column startsWith 'bithon'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
        
        // Now test with startsWith optimization disabled
        RegularExpressionMatchOptimizer disabledStartsWithOptimizer = RegularExpressionMatchOptimizer.of(
            QuerySettings.builder()
                         .enableRegularExpressionOptimization(true)
                         .enableRegularExpressionOptimizationToStartsWith(false)
                         .build()
        );
        
        optimized = disabledStartsWithOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);
        
        // Should not be optimized to startsWith
        assertInstanceOf(ConditionalExpression.RegularExpressionMatchExpression.class, optimized);
        assertEquals("column =~ '^bithon.*'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testContains() {
        IExpression expr = createMatchExpression("abc");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.Contains.class, optimized);
        assertEquals("column contains 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testContainsWithStars() {
        IExpression expr = createMatchExpression(".*abc.*");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.Contains.class, optimized);
        assertEquals("column contains 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testInOperator() {
        IExpression expr = createMatchExpression("^(a|b|c)$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.In.class, optimized);
        assertEquals("column in ('a', 'b', 'c')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testInOperatorWithoutAnchors() {
        IExpression expr = createMatchExpression("(a|b|c)");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.In.class, optimized);
        assertEquals("column in ('a', 'b', 'c')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testEscapedMetacharacters() {
        // Pattern with escaped metacharacters should not be optimized
        IExpression expr = createMatchExpression("a\\.b");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        // Should be optimized to Contains since . is escaped
        assertInstanceOf(ConditionalExpression.Contains.class, optimized);
        assertEquals("column contains 'a\\.b'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testUnescapedMetacharacters() {
        // Pattern with unescaped metacharacters should not be optimized
        IExpression expr = createMatchExpression("a?b");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        // Should remain as RegexMatch
        assertInstanceOf(ConditionalExpression.RegularExpressionMatchExpression.class, optimized);
    }

    @Test
    public void testComplexPattern() {
        // A complex pattern that shouldn't be optimized
        IExpression expr = createMatchExpression("^a(b|c)*d+e?");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.RegularExpressionMatchExpression.class, optimized);
    }

    @Test
    public void testNotMatch_ExactMatch() {
        IExpression expr = createNotMatchExpression("^abc$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.NE.class, optimized);
        assertEquals("column <> 'abc'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNotMatch_StartsWith() {
        IExpression expr = createNotMatchExpression("^abc");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(LogicalExpression.NOT.class, optimized);
        assertEquals("NOT (column startsWith 'abc')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNotMatch_EndsWith() {
        IExpression expr = createNotMatchExpression("abc$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(LogicalExpression.NOT.class, optimized);
        assertEquals("NOT (column endsWith 'abc')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNotMatch_Contains() {
        IExpression expr = createNotMatchExpression("abc");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(LogicalExpression.NOT.class, optimized);
        assertEquals("NOT (column contains 'abc')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNotMatch_In() {
        IExpression expr = createNotMatchExpression("^(a|b|c)$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(ConditionalExpression.NotIn.class, optimized);
        assertEquals("column not in ('a', 'b', 'c')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testSingleDotPattern() {
        IExpression expr = createMatchExpression("a.b");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like '%a_b%'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testSingleDotPatternWithAnchors() {
        IExpression expr = createMatchExpression("^a.b$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like 'a_b'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testSingleDotPatternWithStartAnchor() {
        IExpression expr = createMatchExpression("^a.b");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like 'a_b%'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testSingleDotPatternWithEndAnchor() {
        IExpression expr = createMatchExpression("a.b$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like '%a_b'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testMultipleDotPattern() {
        IExpression expr = createMatchExpression("a.b.c");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like '%a_b_c%'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNotMatch_Like() {
        IExpression expr = createNotMatchExpression("a.b");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(LogicalExpression.NOT.class, optimized);
        assertEquals("NOT (column like '%a_b%')", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testEscapedDotPattern() {
        IExpression expr = createMatchExpression("a\\.b");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        // The dot is escaped, so it should match the literal dot
        assertInstanceOf(ConditionalExpression.Contains.class, optimized);
        assertEquals("column contains 'a\\.b'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testMixedDotAndEscapedDot() {
        IExpression expr = createMatchExpression("a.b\\.c");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like '%a_b.c%'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testDigitsOnlyPattern() {
        IExpression expr = createMatchExpression("^[0-9]+$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.GT.class, optimized);
        assertEquals("column > '0'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testDigitsOnlyPatternWithOptional() {
        IExpression expr = createMatchExpression("^[0-9]*$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.GTE.class, optimized);
        assertEquals("column >= '0'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testDigitsShorthandPattern() {
        IExpression expr = createMatchExpression("^\\d+$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.GT.class, optimized);
        assertEquals("column > '0'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testWordCharPattern() {
        IExpression expr = createMatchExpression("^\\w+$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        assertInstanceOf(LikeOperator.class, optimized);
        assertEquals("column like '[a-zA-Z0-9_]%'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNotMatch_DigitsOnly() {
        IExpression expr = createNotMatchExpression("^\\d+$");
        IExpression optimized = optimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        assertInstanceOf(ComparisonExpression.LTE.class, optimized);
        assertEquals("column <= '0'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testEmptyRHS() {
        // Test with null RHS which should not be optimized
        ConditionalExpression.RegularExpressionMatchExpression expr = new ConditionalExpression.RegularExpressionMatchExpression(testColumn, null);
        IExpression optimized = optimizer.optimize(expr);

        // Should remain as the original expression
        assertInstanceOf(ConditionalExpression.RegularExpressionMatchExpression.class, optimized);
        assertEquals(expr, optimized);
    }

    @Test
    public void testOptimizationDisabled() {
        RegularExpressionMatchOptimizer disabledOptimizer = RegularExpressionMatchOptimizer.of();

        // Try to optimize a simple pattern that would normally be optimized
        IExpression expr = createMatchExpression("^abc$");
        IExpression optimized = disabledOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        // Should remain as the original regex expression
        assertInstanceOf(ConditionalExpression.RegularExpressionMatchExpression.class, optimized);
        assertEquals("column =~ '^abc$'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNotMatchOptimizationDisabled() {
        RegularExpressionMatchOptimizer disabledOptimizer = RegularExpressionMatchOptimizer.of();

        // Try to optimize a simple pattern that would normally be optimized
        IExpression expr = createNotMatchExpression("^abc$");
        IExpression optimized = disabledOptimizer.optimize((ConditionalExpression.RegularExpressionNotMatchExpression) expr);

        // Should remain as the original not-match expression
        assertInstanceOf(ConditionalExpression.RegularExpressionNotMatchExpression.class, optimized);
        assertEquals("column !~ '^abc$'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testNullQuerySettings() {
        // Create optimizer with null settings
        RegularExpressionMatchOptimizer nullSettingsOptimizer = RegularExpressionMatchOptimizer.of(null);

        // Try to optimize a simple pattern
        IExpression expr = createMatchExpression("^abc$");
        IExpression optimized = nullSettingsOptimizer.optimize((ConditionalExpression.RegularExpressionMatchExpression) expr);

        // Should remain as the original expression
        assertInstanceOf(ConditionalExpression.RegularExpressionMatchExpression.class, optimized);
        assertEquals("column =~ '^abc$'", optimized.serializeToText(IdentifierQuotaStrategy.NONE));
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
