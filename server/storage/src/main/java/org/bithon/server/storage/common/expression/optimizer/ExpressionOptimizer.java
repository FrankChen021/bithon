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

package org.bithon.server.storage.common.expression.optimizer;

import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ArrayAccessExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor2;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.MacroExpression;

/**
 * @author Frank Chen
 * @date 5/9/23 9:48 pm
 */
public class ExpressionOptimizer {

    public static IExpression optimize(IExpression expression) {
        return expression.accept(new HasTokenFunctionOptimizer())
                         .accept(new ConstantFoldingOptimizer());
    }

    static class DefaultOptimizer implements IExpressionVisitor2<IExpression> {
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
        public IExpression visit(ArithmeticExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(ComparisonExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(MacroExpression expression) {
            return expression;
        }
    }

    static class ConstantFoldingOptimizer extends DefaultOptimizer {
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
                return new LiteralExpression(expression.evaluate(null));
            } else {
                return expression;
            }
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
            int literalCount = 0;
            for (int i = 0; i < expression.getParameters().size(); i++) {
                IExpression newParameter = expression.getParameters().get(i).accept(this);
                expression.getParameters().set(i, newParameter);

                literalCount += newParameter instanceof LiteralExpression ? 1 : 0;
            }
            if (literalCount == expression.getParameters().size()) {
                return new LiteralExpression(expression.evaluate(null));
            }
            return expression;
        }

        @Override
        public IExpression visit(ArrayAccessExpression expression) {
            IExpression arrayExpression = expression.getArray().accept(this);
            if (arrayExpression instanceof LiteralExpression) {
                Object[] array = ((LiteralExpression) arrayExpression).asArray();
                if (array.length > expression.getIndex()) {
                    return new LiteralExpression(array[expression.getIndex()]);
                } else {
                    throw new RuntimeException("Out of index");
                }
            }
            expression.setArray(arrayExpression);
            return expression;
        }

        @Override
        public IExpression visit(ArithmeticExpression expression) {
            expression.setLeft(expression.getLeft().accept(this));
            expression.setRight(expression.getRight().accept(this));
            if (expression.getLeft() instanceof LiteralExpression && expression.getRight() instanceof LiteralExpression) {
                return new LiteralExpression(expression.evaluate(null));
            }
            return expression;
        }

        @Override
        public IExpression visit(ComparisonExpression expression) {
            expression.setLeft(expression.getLeft().accept(this));
            expression.setRight(expression.getRight().accept(this));
            if (expression.getLeft() instanceof LiteralExpression && expression.getRight() instanceof LiteralExpression) {
                return new LiteralExpression(expression.evaluate(null));
            }
            return expression;
        }
    }

    static class HasTokenFunctionOptimizer extends DefaultOptimizer {
        @Override
        public IExpression visit(FunctionExpression expression) {
            if (!"hasToken".equals(expression.getName())) {
                return expression;
            }

            // The hasToken already checks the parameter is a type of Literal
            String needle = ((LiteralExpression) expression.getParameters().get(1)).asString();

            for (int i = 0, size = needle.length(); i < size; i++) {
                char chr = needle.charAt(i);
                if (isTokenSeparator(chr)) {
                    // replace this function into a LIKE expression
                    return new ComparisonExpression.LIKE(expression.getParameters().get(0),
                                                         new LiteralExpression("%" + needle + "%"));
                }
            }
            return expression;
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
