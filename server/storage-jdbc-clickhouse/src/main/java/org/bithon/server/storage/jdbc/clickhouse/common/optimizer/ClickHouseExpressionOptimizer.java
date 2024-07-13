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

package org.bithon.server.storage.jdbc.clickhouse.common.optimizer;

import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.optimzer.ExpressionOptimizer;
import org.bithon.server.storage.datasource.builtin.Functions;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank chen
 * @date 02/05/24 22:11pm
 */
public class ClickHouseExpressionOptimizer extends ExpressionOptimizer.AbstractOptimizer {

    @Override
    public IExpression visit(ConditionalExpression expression) {
        if (expression instanceof ConditionalExpression.Contains) {
            return new ConditionalExpression.Like(expression.getLeft(),
                                                  LiteralExpression.create("%" + ((LiteralExpression) expression.getRight()).asString() + "%"));
        }

        if (expression instanceof ConditionalExpression.StartsWith) {
            return new FunctionExpression(
                Functions.getInstance().getFunction("startsWith"),
                expression.getLeft(),
                expression.getRight()
            );
        }

        if (expression instanceof ConditionalExpression.EndsWith) {
            return new FunctionExpression(
                Functions.getInstance().getFunction("endsWith"),
                expression.getLeft(),
                expression.getRight()
            );
        }

        return super.visit(expression);
    }

    @Override
    public IExpression visit(FunctionExpression expression) {
        if (!"hasToken".equals(expression.getName())) {
            return expression;
        }
        return new HasTokenFunction().optimize(expression);
    }

    static class HasTokenFunction {
        public IExpression optimize(FunctionExpression expression) {
            // The hasToken already checks the parameter is a type of Literal
            IExpression haystack = expression.getParameters().get(0);
            String needle = ((LiteralExpression) expression.getParameters().get(1)).asString();

            int i = 0;
            int needleLength = needle.length();
            while (i < needleLength && isTokenSeparator(needle.charAt(i))) {
                i++;
            }

            int j = needleLength - 1;
            while (j >= 0 && isTokenSeparator(needle.charAt(j))) {
                j--;
            }
            if (i > 0 && j < needleLength - 1) {
                // This is the case that the needle is surrounded by token separators,
                // CK can use index for such LIKE expression.
                return new ConditionalExpression.Like(haystack,
                                                      LiteralExpression.create("%" + needle + "%"));
            }

            // Otherwise, we try to extract tokens from the needle to turn this function as
            // hasToken() AND xxx LIKE '%needle%'
            List<IExpression> subExpressions = new ArrayList<>();
            int tokenStart = 0;
            for (i = 0; i < needleLength; i++) {
                char chr = needle.charAt(i);
                if (isTokenSeparator(chr)) {
                    if (i > tokenStart) {
                        IExpression literal = LiteralExpression.create(needle.substring(tokenStart, i));
                        subExpressions.add(new FunctionExpression(expression.getFunction(), haystack, literal));
                    }

                    // Since the current character is a token separator,
                    // let's start with the next character
                    tokenStart = i + 1;
                }
            }
            if (tokenStart > 0 && tokenStart < needleLength) {
                IExpression literal = LiteralExpression.create(needle.substring(tokenStart));
                subExpressions.add(new FunctionExpression(expression.getFunction(), haystack, literal));
            }
            if (subExpressions.isEmpty()) {
                // No token separator in the needle, no need to optimize
                return expression;
            }

            subExpressions.add(new ConditionalExpression.Like(haystack,
                                                              LiteralExpression.create("%" + needle + "%")));

            return new LogicalExpression.AND(subExpressions);
        }

        /**
         * ALWAYS_INLINE static bool isTokenSeparator(const uint8_t c)
         * {
         * return !(isAlphaNumericASCII(c) || !isASCII(c));
         * }
         */
        private boolean isTokenSeparator(char chr) {
            return !(Character.isLetterOrDigit(chr) || (chr > 127));
        }
    }
}
