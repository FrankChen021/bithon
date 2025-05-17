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
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.bithon.server.datasource.reader.jdbc.dialect.LikeOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turn regular expression match into startsWith/endsWith which has better performance in CK
 *
 * @author frank.chen021@outlook.com
 * @date 14/5/25 11:09 am
 */
public class RegularExpressionMatchOptimizer {
    private final QuerySettings querySettings;

    private RegularExpressionMatchOptimizer(QuerySettings querySettings) {
        this.querySettings = querySettings;
    }

    /**
     * Factory method that creates an optimizer with given query settings
     */
    public static RegularExpressionMatchOptimizer of(QuerySettings querySettings) {
        return new RegularExpressionMatchOptimizer(querySettings);
    }

    /**
     * Factory method that creates an optimizer with default settings
     */
    public static RegularExpressionMatchOptimizer of() {
        return of(null);
    }

    // Add a method for not match expressions
    IExpression optimize(ConditionalExpression.RegularExpressionNotMatchExpression expression) {
        if (querySettings == null || !querySettings.isEnableRegularExpressionOptimization()) {
            return expression;
        }

        if (!(expression.getRhs() instanceof LiteralExpression.StringLiteral)) {
            return expression;
        }
        // First optimize the match expression
        IExpression optimized = optimize(
            new ConditionalExpression.RegularExpressionMatchExpression(expression.getLhs(), expression.getRhs())
        );

        // If the result was optimized to something other than RegularExpressionMatchExpression,
        // we need to negate it
        if (!(optimized instanceof ConditionalExpression.RegularExpressionMatchExpression)) {
            if (optimized instanceof ComparisonExpression.EQ eq) {
                return new ComparisonExpression.NE(eq.getLhs(), eq.getRhs());
            } else if (optimized instanceof ConditionalExpression.StartsWith sw) {
                // We would need a NotStartsWith class, or use logical negation
                return new LogicalExpression.NOT(sw);
            } else if (optimized instanceof ConditionalExpression.EndsWith ew) {
                return new LogicalExpression.NOT(ew);
            } else if (optimized instanceof ConditionalExpression.Contains c) {
                return new LogicalExpression.NOT(c);
            } else if (optimized instanceof ConditionalExpression.In in) {
                return new ConditionalExpression.NotIn(in.getLhs(), (ExpressionList) in.getRhs());
            } else if (optimized instanceof LikeOperator like) {
                // Convert LIKE to NOT LIKE
                return new LogicalExpression.NOT(like);
            } else if (optimized instanceof ComparisonExpression.GT gt) {
                return new ComparisonExpression.LTE(gt.getLhs(), gt.getRhs());
            } else if (optimized instanceof ComparisonExpression.LT lt) {
                return new ComparisonExpression.GTE(lt.getLhs(), lt.getRhs());
            }
        }

        // If no optimization was applied, return the original not-match expression
        return expression;
    }

    IExpression optimize(ConditionalExpression.RegularExpressionMatchExpression expression) {
        if (querySettings == null || !querySettings.isEnableRegularExpressionOptimization()) {
            return expression;
        }

        IExpression lhs = expression.getLhs();
        IExpression rhs = expression.getRhs();
        if (!(rhs instanceof LiteralExpression.StringLiteral)) {
            return expression;
        }
        String pattern = ((LiteralExpression.StringLiteral) rhs).getValue();

        try {
            // Handle empty pattern
            if (pattern.isEmpty()) {
                return new ComparisonExpression.EQ(lhs, new LiteralExpression.StringLiteral(""));
            }

            // Remove anchors for analysis but remember them
            boolean startsWithCaret = pattern.startsWith("^");
            boolean endsWithDollar = pattern.endsWith("$");
            String unanchoredPattern = pattern;
            if (startsWithCaret) {
                unanchoredPattern = unanchoredPattern.substring(1);
            }
            if (endsWithDollar && !unanchoredPattern.isEmpty()) {
                unanchoredPattern = unanchoredPattern.substring(0, unanchoredPattern.length() - 1);
            }

            // Check for unescaped metacharacters
            if (!containsUnescapedMetachars(unanchoredPattern)) {
                if (startsWithCaret && endsWithDollar) {
                    // Exact match if anchored on both sides
                    return new ComparisonExpression.EQ(lhs, new LiteralExpression.StringLiteral(unanchoredPattern));
                } else if (startsWithCaret) {
                    return querySettings.isEnableRegularExpressionToStartsWith() ? new ConditionalExpression.StartsWith(lhs, new LiteralExpression.StringLiteral(unanchoredPattern))
                                                                                 : expression;
                } else if (endsWithDollar) {
                    return querySettings.isEnableRegularExpressionToEndsWith() ? new ConditionalExpression.EndsWith(lhs, new LiteralExpression.StringLiteral(unanchoredPattern))
                                                                               : expression;
                } else {
                    // Contains if no anchors
                    // Contains expression will be turned into LIKE if using Expression2Sql
                    return new ConditionalExpression.Contains(lhs, new LiteralExpression.StringLiteral(unanchoredPattern));
                }
            }

            // Check for number range patterns: ^[0-9]+$, ^\d+$
            if (startsWithCaret && endsWithDollar &&
                (unanchoredPattern.equals("[0-9]+") || unanchoredPattern.equals("\\d+") ||
                 unanchoredPattern.equals("[0-9]*") || unanchoredPattern.equals("\\d*"))) {
                // Can't do exact optimization but we can use > 0 or >= 0 based on + or *
                boolean isZeroAllowed = unanchoredPattern.contains("*");
                if (isZeroAllowed) {
                    return new ComparisonExpression.GTE(lhs, new LiteralExpression.StringLiteral("0"));
                } else {
                    return new ComparisonExpression.GT(lhs, new LiteralExpression.StringLiteral("0"));
                }
            }

            // startsWith: "^prefix.*" or "^prefix"
            if (startsWithCaret && unanchoredPattern.endsWith(".*")) {
                if (querySettings.isEnableRegularExpressionToStartsWith()) {
                    String prefix = unanchoredPattern.substring(0, unanchoredPattern.length() - 2);
                    if (!containsUnescapedMetachars(prefix)) {
                        return new ConditionalExpression.StartsWith(lhs, new LiteralExpression.StringLiteral(prefix));
                    }
                } else {
                    return expression;
                }
            }

            // endsWith: ".*suffix$" or "suffix$"
            if (endsWithDollar && unanchoredPattern.startsWith(".*")) {
                if (querySettings.isEnableRegularExpressionToEndsWith()) {
                    String suffix = unanchoredPattern.substring(2);
                    if (!containsUnescapedMetachars(suffix)) {
                        return new ConditionalExpression.EndsWith(lhs, new LiteralExpression.StringLiteral(suffix));
                    }
                } else {
                    return expression;
                }
            }

            // contains: ".*sub.*"
            if (unanchoredPattern.startsWith(".*") && unanchoredPattern.endsWith(".*")) {
                String sub = unanchoredPattern.substring(2, unanchoredPattern.length() - 2);
                if (!containsUnescapedMetachars(sub)) {
                    return new ConditionalExpression.Contains(lhs, new LiteralExpression.StringLiteral(sub));
                }
            }

            // in: "^(a|b|c)$" or "(a|b|c)" - but only if it's a simple alternation
            if ((startsWithCaret && endsWithDollar && unanchoredPattern.startsWith("(") && unanchoredPattern.endsWith(")")) ||
                (unanchoredPattern.startsWith("(") && unanchoredPattern.endsWith(")"))) {

                String content = unanchoredPattern.substring(1, unanchoredPattern.length() - 1);
                // Simple check that it's only alternation without nested expressions
                if (!content.contains("(") && !content.contains(")") &&
                    !content.contains("[") && !content.contains("]") &&
                    !content.contains("*") && !content.contains("+") &&
                    !content.contains("?")) {

                    // Split on unescaped |
                    List<String> alternatives = splitUnescaped(content, '|');
                    if (!alternatives.isEmpty() && alternatives.stream().noneMatch(RegularExpressionMatchOptimizer::containsUnescapedMetachars)) {
                        List<IExpression> items = alternatives.stream()
                                                              .map(LiteralExpression.StringLiteral::new)
                                                              .collect(Collectors.toList());
                        return new ConditionalExpression.In(lhs, new ExpressionList(items));
                    }
                }
            }

            // Pattern with single dots should be converted to LIKE with underscore
            if (containsOnlySingleDotWildcards(pattern)) {
                String likePattern = pattern;

                // First handle escaped . in pattern so they don't get converted to LIKE wildcards
                StringBuilder sb = new StringBuilder();
                boolean escaped = false;
                for (int i = 0; i < likePattern.length(); i++) {
                    char c = likePattern.charAt(i);
                    if (escaped) {
                        if (c == '.') {
                            // This is an escaped dot, we'll replace it with a special marker temporarily
                            sb.append("\u0000"); // Use null character as marker
                        } else {
                            // Other escaped chars we keep as is
                            sb.append('\\').append(c);
                        }
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else {
                        sb.append(c);
                    }
                }
                likePattern = sb.toString();

                // Now replace all unescaped dots with underscores
                likePattern = likePattern.replace(".", "_");

                // Put back the escaped dots
                likePattern = likePattern.replace("\u0000", ".");

                if (startsWithCaret) {
                    likePattern = likePattern.substring(1);
                }
                if (endsWithDollar && likePattern.length() > 0) {
                    likePattern = likePattern.substring(0, likePattern.length() - 1);
                }

                // Add % wildcards based on anchors
                if (!startsWithCaret) {
                    likePattern = "%" + likePattern;
                }
                if (!endsWithDollar) {
                    likePattern = likePattern + "%";
                }

                // Escape special characters in the LIKE pattern ('%' and '_')
                // that are not meant to be wildcards but actual literals
                likePattern = escapeLikeLiterals(likePattern);

                return new LikeOperator(lhs, new LiteralExpression.StringLiteral(likePattern));
            }

            // Handle common character classes: \d, \w, \s
            if (pattern.contains("\\d") || pattern.contains("\\w") || pattern.contains("\\s")) {
                // Only optimize simple patterns like "^\d+$" or "^\w+$"
                if (startsWithCaret && endsWithDollar) {
                    if (unanchoredPattern.equals("\\d+")) {
                        // Pattern is exactly "^\d+$" - digits only
                        return new LikeOperator(lhs, new LiteralExpression.StringLiteral("[0-9]%"));
                    } else if (unanchoredPattern.equals("\\w+")) {
                        // Pattern is exactly "^\w+$" - word chars only
                        return new LikeOperator(lhs, new LiteralExpression.StringLiteral("[a-zA-Z0-9_]%"));
                    }
                }
            }
        } catch (Exception e) {
            // If anything goes wrong in optimization, fall back to original expression
            // Consider logging the exception if needed
        }

        // fallback
        return expression;
    }

    /**
     * Prepares a LIKE pattern by ensuring that the wildcards are properly maintained
     * while escaping any literal wildcard characters that should be matched exactly.
     * <p>
     * This method handles two types of special LIKE characters:
     * - % (percent): Matches any sequence of characters
     * - _ (underscore): Matches exactly one character
     * <p>
     * The key rules are:
     * 1. Don't escape % at the beginning or end of pattern (these are intentional wildcards)
     * 2. Don't escape _ characters (these were converted from . in regex)
     * 3. Escape any other '%' or '_' characters that should be matched literally
     * <p>
     * Examples:
     * - Input: "%a_b%" → Output: "%a_b%" (maintains wildcards)
     * - Input: "a%b" → Output: "a\\%b" (escapes the % in the middle)
     * - Input: "a_b" → Output: "a_b" (keeps underscore from regex dot conversion)
     * - Input: "%100\\_percent%" → Output: "%100\\_percent%" (keeps existing escapes)
     *
     * @param pattern The LIKE pattern with potential wildcards
     * @return A properly escaped LIKE pattern
     */
    private static String escapeLikeLiterals(String pattern) {
        // We don't escape the % wildcards at start/end or the _ wildcards from dots
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '%' || c == '_') {
                // Don't escape the % at the beginning and end of the pattern - these are our wildcards
                if (c == '%' && (i == 0 || i == pattern.length() - 1)) {
                    sb.append(c);
                    continue;
                }

                // Don't escape the _ that were converted from dots in the regex pattern
                if (c == '_') {
                    sb.append(c);
                    continue;
                }

                // Otherwise escape the character
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean containsUnescapedMetachars(String pattern) {
        boolean escaped = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (".*+?|()[]{}^$".indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitUnescaped(String input, char delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaped) {
                current.append('\\').append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == delimiter) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        if (escaped) {
            // Handle case where \ is the last character
            current.append('\\');
        }

        if (!current.isEmpty()) {
            result.add(current.toString());
        }

        return result;
    }

    private static boolean containsOnlySingleDotWildcards(String pattern) {
        // Remove anchors before checking
        String processedPattern = pattern;
        if (processedPattern.startsWith("^")) {
            processedPattern = processedPattern.substring(1);
        }
        if (processedPattern.endsWith("$")) {
            processedPattern = processedPattern.substring(0, processedPattern.length() - 1);
        }

        // Now check for complex regex patterns
        if (processedPattern.contains(".*") || processedPattern.contains("*.") ||
            processedPattern.contains("+") || processedPattern.contains("?") ||
            processedPattern.contains("[") || processedPattern.contains("]") ||
            processedPattern.contains("(") || processedPattern.contains(")") ||
            processedPattern.contains("{") || processedPattern.contains("}")) {
            return false;
        }

        // Track escaping to properly identify unescaped dots
        boolean escaped = false;
        boolean hasUnescapedDot = false;

        for (int i = 0; i < processedPattern.length(); i++) {
            char c = processedPattern.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            // Only allow dot as a regex special character
            if (c == '.') {
                hasUnescapedDot = true;
            } else if ("*+?|()[]{}^$".indexOf(c) >= 0) {
                return false;
            }
        }
        return hasUnescapedDot; // Must contain at least one unescaped dot
    }
}