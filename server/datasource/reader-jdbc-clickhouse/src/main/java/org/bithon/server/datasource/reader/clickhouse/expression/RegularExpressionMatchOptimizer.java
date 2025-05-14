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
import org.bithon.server.datasource.reader.jdbc.dialect.LikeOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 14/5/25 11:09 am
 */
class RegularExpressionMatchOptimizer {

    // Add a method for not match expressions
    static IExpression optimize(ConditionalExpression.RegularExpressionNotMatchExpression expression) {
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
            }
        }

        // If no optimization was applied, return the original not-match expression
        return expression;
    }

    static IExpression optimize(ConditionalExpression.RegularExpressionMatchExpression expression) {
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
                    return new ConditionalExpression.StartsWith(lhs, new LiteralExpression.StringLiteral(unanchoredPattern));
                } else if (endsWithDollar) {
                    return new ConditionalExpression.EndsWith(lhs, new LiteralExpression.StringLiteral(unanchoredPattern));
                } else {
                    // Contains if no anchors
                    // Contains expression will be turned into LIKE if using Expression2Sql
                    return new ConditionalExpression.Contains(lhs, new LiteralExpression.StringLiteral(unanchoredPattern));
                }
            }

            // startsWith: "^prefix.*" or "^prefix"
            if (startsWithCaret && unanchoredPattern.endsWith(".*")) {
                String prefix = unanchoredPattern.substring(0, unanchoredPattern.length() - 2);
                if (!containsUnescapedMetachars(prefix)) {
                    return new ConditionalExpression.StartsWith(lhs, new LiteralExpression.StringLiteral(prefix));
                }
            }

            // endsWith: ".*suffix$" or "suffix$"
            if (endsWithDollar && unanchoredPattern.startsWith(".*")) {
                String suffix = unanchoredPattern.substring(2);
                if (!containsUnescapedMetachars(suffix)) {
                    return new ConditionalExpression.EndsWith(lhs, new LiteralExpression.StringLiteral(suffix));
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
                String likePattern = pattern.replace(".", "_");
                if (startsWithCaret) {
                    likePattern = likePattern.substring(1);
                }
                if (endsWithDollar) {
                    likePattern = likePattern.substring(0, likePattern.length() - 1);
                }

                // Add % wildcards based on anchors
                if (!startsWithCaret) {
                    likePattern = "%" + likePattern;
                }
                if (!endsWithDollar) {
                    likePattern = likePattern + "%";
                }

                return new LikeOperator(lhs, new LiteralExpression.StringLiteral(likePattern));
            }
        } catch (Exception e) {
            // If anything goes wrong in optimization, fall back to original expression
            // Consider logging the exception if needed
        }

        // fallback
        return expression;
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
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                current.append(c);
                escaped = true;
            } else if (c == delimiter) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
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

        boolean escaped = false;
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
            if ("*+?|()[]{}^$".indexOf(c) >= 0) {
                return false;
            }
        }
        return processedPattern.contains("."); // Must contain at least one dot
    }
}