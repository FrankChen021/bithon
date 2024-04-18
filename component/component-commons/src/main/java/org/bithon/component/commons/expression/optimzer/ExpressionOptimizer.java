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

package org.bithon.component.commons.expression.optimzer;

import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ArrayAccessExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor2;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.expression.MapAccessExpression;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Frank Chen
 * @date 5/9/23 9:48 pm
 */
public class ExpressionOptimizer {

    public static IExpression optimize(IExpression expression) {
        return expression.accept(new HasTokenFunctionOptimizer())
                         .accept(new ConstantFoldingOptimizer())
                         .accept(new LogicalExpressionOptimizer());
    }

    public static class AbstractOptimizer implements IExpressionVisitor2<IExpression> {
        @Override
        public IExpression visit(LiteralExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(LogicalExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(IdentifierExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(ExpressionList expression) {
            for (int i = 0; i < expression.getExpressions().size(); i++) {
                IExpression newSubExpression = expression.getExpressions().get(i);
                expression.getExpressions().set(i, newSubExpression);
            }
            return expression;
        }

        @Override
        public IExpression visit(FunctionExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(ArrayAccessExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(MapAccessExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(ArithmeticExpression expression) {
            expression.setLeft(expression.getLeft().accept(this));
            expression.setRight(expression.getRight().accept(this));
            return expression;
        }

        @Override
        public IExpression visit(ConditionalExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(MacroExpression expression) {
            return expression;
        }
    }

    static class ConstantFoldingOptimizer extends AbstractOptimizer {
        @Override
        public IExpression visit(LogicalExpression expression) {
            int literalCount = 0;

            for (int i = 0; i < expression.getOperands().size(); i++) {
                IExpression newOperand = expression.getOperands().get(i).accept(this);
                expression.getOperands().set(i, newOperand);

                literalCount += newOperand instanceof LiteralExpression ? 1 : 0;
            }

            if (literalCount == expression.getOperands().size()) {
                // All sub expressions are literal, do constant folding
                return LiteralExpression.create(expression.evaluate(null));
            } else {
                return expression;
            }
        }

        @Override
        public IExpression visit(FunctionExpression expression) {
            int literalCount = 0;
            for (int i = 0; i < expression.getParameters().size(); i++) {
                IExpression newParameter = expression.getParameters().get(i).accept(this);
                expression.getParameters().set(i, newParameter);

                literalCount += newParameter instanceof LiteralExpression ? 1 : 0;
            }
            if (literalCount == expression.getParameters().size()) {
                return LiteralExpression.create(expression.evaluate(null));
            }
            return expression;
        }

        @Override
        public IExpression visit(ArrayAccessExpression expression) {
            IExpression arrayExpression = expression.getArray().accept(this);
            if (arrayExpression instanceof LiteralExpression) {
                Object[] array = ((LiteralExpression) arrayExpression).asArray();
                if (array.length > expression.getIndex()) {
                    return LiteralExpression.create(array[expression.getIndex()]);
                } else {
                    throw new RuntimeException("Out of index");
                }
            }
            expression.setArray(arrayExpression);
            return expression;
        }

        @Override
        public IExpression visit(ArithmeticExpression expression) {
            super.visit(expression);

            if (expression.getLeft() instanceof LiteralExpression && expression.getRight() instanceof LiteralExpression) {
                return LiteralExpression.create(expression.evaluate(null));
            }
            return expression;
        }

        @Override
        public IExpression visit(ConditionalExpression expression) {
            expression.setLeft(expression.getLeft().accept(this));
            expression.setRight(expression.getRight().accept(this));
            if (expression.getLeft() instanceof LiteralExpression && expression.getRight() instanceof LiteralExpression) {
                return LiteralExpression.create(expression.evaluate(null));
            }
            return expression;
        }
    }

    /**
     * TODO: Move this to CK dialect
     */
    static class HasTokenFunctionOptimizer extends AbstractOptimizer {
        @Override
        public IExpression visit(LogicalExpression expression) {
            expression.getOperands().replaceAll(iExpression -> iExpression.accept(this));
            return expression;
        }

        @Override
        public IExpression visit(ConditionalExpression expression) {
            expression.setLeft(expression.getLeft().accept(this));
            expression.setRight(expression.getRight().accept(this));
            return expression;
        }

        @Override
        public IExpression visit(FunctionExpression expression) {
            if (!"hasToken".equals(expression.getName())) {
                return expression;
            }

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

    /**
     * Simplifies constant expressions in logical AND/OR/NOT.
     * For example, the expression '1 = 1 AND condition2' can be simplified as condition2.
     * '1 = 1 OR condition2' can be simplified as true.
     */
    static class LogicalExpressionOptimizer extends AbstractOptimizer {
        @Override
        public IExpression visit(LogicalExpression expression) {
            expression.getOperands().replaceAll(iExpression -> iExpression.accept(this));

            if (expression instanceof LogicalExpression.AND) {
                return handleAndExpression((LogicalExpression.AND) expression);
            }
            if (expression instanceof LogicalExpression.OR) {
                return handleOrExpression((LogicalExpression.OR) expression);
            }
            if (expression instanceof LogicalExpression.NOT) {
                return handleNotExpression(expression);
            }
            return expression;
        }

        private IExpression handleNotExpression(LogicalExpression expression) {
            // When the sub expression is optimized into one expression,
            // we can only apply the optimization to it again for simplicity.
            if (expression.getOperands().size() != 1) {
                return expression;
            }

            IExpression subExpression = expression.getOperands().get(0);
            if (!(subExpression instanceof LiteralExpression)) {
                return expression;
            }

            if (((LiteralExpression) subExpression).isNumber()) {
                if (((LiteralExpression) subExpression).asBoolean()) {
                    // the sub expression is true, the whole expression is false
                    return LiteralExpression.create(false);
                } else {
                    return LiteralExpression.create(true);
                }
            } else if (IDataType.BOOLEAN.equals(subExpression.getDataType())) {
                if (((LiteralExpression) subExpression).asBoolean()) {
                    // the sub expression is true, the whole expression is false
                    return LiteralExpression.create(false);
                } else {
                    return LiteralExpression.create(true);
                }
            }
            return expression;
        }

        private IExpression handleOrExpression(LogicalExpression.OR expression) {
            Iterator<IExpression> subExpressionIterator = expression.getOperands().iterator();

            while (subExpressionIterator.hasNext()) {
                IExpression subExpression = subExpressionIterator.next();

                if (!(subExpression instanceof LiteralExpression)) {
                    continue;
                }

                if (((LiteralExpression) subExpression).isNumber()) {
                    if (((LiteralExpression) subExpression).asBoolean()) {
                        // the sub expression is true, the whole expression is true
                        return LiteralExpression.create(true);
                    } else {
                        // The sub expression is false, it should be removed from the expression list
                        subExpressionIterator.remove();

                    }
                } else if (IDataType.BOOLEAN.equals(subExpression.getDataType())) {
                    if (((LiteralExpression) subExpression).asBoolean()) {
                        // the sub expression is true, the whole expression is true
                        return subExpression;
                    } else {
                        // The sub expression is false, it should be removed from the expression list
                        subExpressionIterator.remove();
                    }
                }
            }

            int subExprSize = expression.getOperands().size();
            if (subExprSize == 0) {
                return LiteralExpression.create(true);
            }
            if (subExprSize == 1) {
                return expression.getOperands().get(0);
            }
            return expression;
        }

        private IExpression handleAndExpression(LogicalExpression.AND andExpression) {
            Iterator<IExpression> subExpressionIterator = andExpression.getOperands().iterator();

            while (subExpressionIterator.hasNext()) {
                IExpression subExpression = subExpressionIterator.next();

                if (!(subExpression instanceof LiteralExpression)) {
                    continue;
                }

                if (((LiteralExpression) subExpression).isNumber()) {
                    if (((LiteralExpression) subExpression).asBoolean()) {
                        // true, remove this always true expression
                        subExpressionIterator.remove();
                    } else {
                        // The sub expression is false, the whole expression is FALSE
                        return LiteralExpression.create(false);
                    }
                } else if (IDataType.BOOLEAN.equals(subExpression.getDataType())) {
                    if (((LiteralExpression) subExpression).asBoolean()) {
                        // sub expression is true, remove it from the AND expression
                        subExpressionIterator.remove();
                    } else {
                        // The sub expression is false, the whole expression is FALSE
                        return subExpression;
                    }
                }
            }

            int subExprSize = andExpression.getOperands().size();
            if (subExprSize == 0) {
                return LiteralExpression.create(true);
            }
            if (subExprSize == 1) {
                return andExpression.getOperands().get(0);
            }
            return andExpression;
        }
    }
}
