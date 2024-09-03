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
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.optimzer.ExpressionOptimizer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank chen
 * @date 02/05/24 22:11pm
 */
public class ClickHouseExpressionOptimizer extends ExpressionOptimizer.AbstractOptimizer {

    @Override
    public IExpression visit(ConditionalExpression expression) {
        if (expression instanceof ConditionalExpression.StartsWith) {
            return new FunctionExpression(
                Functions.getInstance().getFunction("startsWith"),
                expression.getLhs(),
                expression.getRhs()
            );
        }

        if (expression instanceof ConditionalExpression.EndsWith) {
            return new FunctionExpression(
                Functions.getInstance().getFunction("endsWith"),
                expression.getLhs(),
                expression.getRhs()
            );
        }

        return super.visit(expression);
    }

    @Override
    public IExpression visit(FunctionExpression expression) {
        if (!"hasToken".equals(expression.getName())) {
            return expression;
        }
        return HasTokenFunctionOptimizer.optimize(expression);
    }

    static class HasTokenFunctionOptimizer {
        static IExpression optimize(FunctionExpression expression) {
            // The hasToken already checks the parameter is a type of Literal
            IExpression input = expression.getArgs().get(0);

            String pattern = ((LiteralExpression<?>) expression.getArgs().get(1)).asString();

            int i = 0;
            int needleLength = pattern.length();
            while (i < needleLength && isTokenSeparator(pattern.charAt(i))) {
                i++;
            }

            int j = needleLength - 1;
            while (j >= 0 && isTokenSeparator(pattern.charAt(j))) {
                j--;
            }
            if (i > 0 && j < needleLength - 1) {
                // This is the case that the needle is surrounded by token separators,
                // CK can use index for such LIKE expression.
                return new ConditionalExpression.Like(input,
                                                      LiteralExpression.ofString("%" + pattern + "%"));
            }

            // Otherwise, we try to extract tokens from the needle to turn this function as
            // hasToken() AND xxx LIKE '%needle%'
            List<IExpression> subExpressions = new ArrayList<>();
            int tokenStart = 0;
            for (i = 0; i < needleLength; i++) {
                char chr = pattern.charAt(i);
                if (isTokenSeparator(chr)) {
                    if (i > tokenStart) {
                        IExpression literal = LiteralExpression.ofString(pattern.substring(tokenStart, i));
                        subExpressions.add(new FunctionExpression(expression.getFunction(), input, literal));
                    }

                    // Since the current character is a token separator,
                    // let's start with the next character
                    tokenStart = i + 1;
                }
            }
            if (tokenStart > 0 && tokenStart < needleLength) {
                IExpression literal = LiteralExpression.ofString(pattern.substring(tokenStart));
                subExpressions.add(new FunctionExpression(expression.getFunction(), input, literal));
            }
            if (subExpressions.isEmpty()) {
                // No token separator in the needle, no need to optimize
                return expression;
            }

            subExpressions.add(new ConditionalExpression.Like(input,
                                                              LiteralExpression.ofString("%" + pattern + "%")));

            return new LogicalExpression.AND(subExpressions);
        }

        /**
         * <code><pre>
         * ALWAYS_INLINE static bool isTokenSeparator(const uint8_t c)
         * {
         *      return !(isAlphaNumericASCII(c) || !isASCII(c));
         * }
         * </pre></code>
         */
        private static boolean isTokenSeparator(char chr) {
            return !(Character.isLetterOrDigit(chr) || (chr > 127));
        }
    }
}
