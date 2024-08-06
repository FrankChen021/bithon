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
import org.bithon.component.commons.expression.ComparisonExpression;
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
import org.bithon.component.commons.expression.TernaryExpression;
import org.bithon.component.commons.expression.function.builtin.TimeFunction;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Frank Chen
 * @date 5/9/23 9:48 pm
 */
public class ExpressionOptimizer {

    public static IExpression optimize(IExpression expression) {
        return expression.accept(new ConstantFunctionOptimizer())
                         .accept(new LogicalExpressionOptimizer())
                         .accept(new ConstantFoldingOptimizer());
    }

    public static class AbstractOptimizer implements IExpressionVisitor2<IExpression> {
        @Override
        public IExpression visit(LiteralExpression expression) {
            return expression;
        }

        /**
         * Flatten Logical Expression
         */
        @Override
        public IExpression visit(LogicalExpression expression) {
            List<IExpression> operands = expression.getOperands();

            for (int i = 0; i < operands.size(); i++) {
                // Apply optimization on the operand
                IExpression subExpression = operands.get(i).accept(this);

                if (subExpression == null) {
                    operands.remove(i);
                    i--;
                } else if (expression instanceof LogicalExpression.AND && subExpression instanceof LogicalExpression.AND
                    || (expression instanceof LogicalExpression.OR && subExpression instanceof LogicalExpression.OR)
                    || (expression instanceof LogicalExpression.NOT && subExpression instanceof LogicalExpression.AND)
                ) {
                    operands.remove(i);

                    // Flatten nested AND/OR expression
                    List<IExpression> nestedExpressions = ((LogicalExpression) subExpression).getOperands();
                    for (IExpression nest : nestedExpressions) {
                        operands.add(i++, nest);
                    }

                    // The nested has N elements, since we remove one element first,
                    // the number total added elements is N - 1
                    i--;
                } else {
                    operands.set(i, subExpression);
                }
            }

            if (!(expression instanceof LogicalExpression.NOT) && operands.size() == 1) {
                return expression.getOperands().get(0);
            }

            return expression;
        }

        @Override
        public IExpression visit(IdentifierExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(ExpressionList expression) {
            optimizeExpressionList(expression.getExpressions());
            return expression;
        }

        @Override
        public IExpression visit(FunctionExpression expression) {
            optimizeExpressionList(expression.getArgs());
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
            expression.setLeft(expression.getLeft().accept(this));
            expression.setRight(expression.getRight().accept(this));
            return expression;
        }

        @Override
        public IExpression visit(MacroExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(TernaryExpression expression) {
            expression.setConditionExpression(expression.getConditionExpression().accept(this));
            expression.setTrueExpression(expression.getTrueExpression().accept(this));
            expression.setFalseExpression(expression.getFalseExpression().accept(this));
            if (expression.getConditionExpression() instanceof LiteralExpression) {
                if (((LiteralExpression) (expression.getConditionExpression())).asBoolean()) {
                    return expression.getTrueExpression();
                } else {
                    return expression.getFalseExpression();
                }
            }
            return expression;
        }

        private void optimizeExpressionList(List<IExpression> expressions) {
            final ListIterator<IExpression> iterator = expressions.listIterator();
            while (iterator.hasNext()) {
                IExpression optimized = iterator.next().accept(this);
                if (optimized == null) {
                    iterator.remove();
                } else {
                    iterator.set(optimized);
                }
            }
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
                // All operands are literal, do constant folding
                return LiteralExpression.create(expression.evaluate(null));
            } else {
                return expression;
            }
        }

        /**
         * Optimize the function expression by folding the constant parameters
         */
        @Override
        public IExpression visit(FunctionExpression expression) {
            if (expression.getFunction().isAggregator()) {
                return expression;
            }
            if (!expression.getFunction().isDeterministic()) {
                return expression;
            }

            int literalCount = 0;
            for (int i = 0; i < expression.getArgs().size(); i++) {
                IExpression newParameter = expression.getArgs().get(i).accept(this);
                expression.getArgs().set(i, newParameter);

                literalCount += newParameter instanceof LiteralExpression ? 1 : 0;
            }
            if (literalCount == expression.getArgs().size()) {
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
     * 1. Simplifies constant expressions in logical AND/OR/NOT.
     * For example, the expression '1 = 1 AND condition2' can be simplified as condition2.
     * '1 = 1 OR condition2' can be simplified as true.
     * 2. Reverse the logical expressions.
     * For example, NOT a = 1 will be optimized into a != 1
     */
    static class LogicalExpressionOptimizer extends AbstractOptimizer {
        @Override
        public IExpression visit(LogicalExpression expression) {
            expression.getOperands().replaceAll(iExpression -> iExpression.accept(this));

            // Check if the Logical expression is a constant expression
            // or simplify the expression if it has only one operand
            // or reverse the logical condition
            if (expression instanceof LogicalExpression.AND) {
                return simplifyAndExpression(expression);
            }
            if (expression instanceof LogicalExpression.OR) {
                return simplifyOrExpression((LogicalExpression.OR) expression);
            }
            if (expression instanceof LogicalExpression.NOT) {
                return handleNotExpression(expression);
            }
            return expression;
        }

        private IExpression handleNotExpression(LogicalExpression expression) {
            // When the sub expression is optimized into one expression,
            // we can only apply the optimization to it again for simplicity.
            if (expression.getOperands().size() > 1) {
                IExpression simplified = simplifyAndExpression(expression);
                if (simplified instanceof LiteralExpression.BooleanLiteral) {
                    return ((LiteralExpression.BooleanLiteral) simplified).negate();
                }

                return expression;
            }

            IExpression subExpression = expression.getOperands().get(0);
            if ((subExpression instanceof LiteralExpression)) {
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
            } else if (subExpression instanceof ConditionalExpression.NotIn) {
                // Turn the expression: 'NOT var not in ('xxx')' into 'var in (xxx)'
                return new ConditionalExpression.In(((ConditionalExpression.NotIn) subExpression).getLeft(),
                                                    (ExpressionList) ((ConditionalExpression.NotIn) subExpression).getRight());
            } else if (subExpression instanceof ConditionalExpression.NotLike) {
                // Turn the expression: 'NOT var not like 'xxx'' into 'var like (xxx)'
                return new ConditionalExpression.Like(((ConditionalExpression.NotLike) subExpression).getLeft(),
                                                      ((ConditionalExpression.NotLike) subExpression).getRight());
            } else if (subExpression instanceof ConditionalExpression.In) {
                // Turn into In into NotIn
                return new ConditionalExpression.NotIn(((ConditionalExpression.In) subExpression).getLeft(),
                                                       (ExpressionList) ((ConditionalExpression.In) subExpression).getRight());
            } else if (subExpression instanceof ConditionalExpression.Like) {
                // Turn into Like into NotLike
                return new ConditionalExpression.NotLike(((ConditionalExpression.Like) subExpression).getLeft(),
                                                         ((ConditionalExpression.Like) subExpression).getRight());
            } else if (subExpression instanceof ComparisonExpression.EQ) {
                // Turn '=' into '<>'
                return new ComparisonExpression.NE(((ComparisonExpression.EQ) subExpression).getLeft(),
                                                   ((ComparisonExpression.EQ) subExpression).getRight());
            } else if (subExpression instanceof ComparisonExpression.NE) {
                // Turn '<>' into '='
                return new ComparisonExpression.EQ(((ComparisonExpression.NE) subExpression).getLeft(),
                                                   ((ComparisonExpression.NE) subExpression).getRight());
            } else if (subExpression instanceof ComparisonExpression.LT) {
                // Turn '<' into '>='
                return new ComparisonExpression.GTE(((ComparisonExpression.LT) subExpression).getLeft(),
                                                    ((ComparisonExpression.LT) subExpression).getRight());
            } else if (subExpression instanceof ComparisonExpression.GT) {
                // Turn '>' into '<='
                return new ComparisonExpression.LTE(((ComparisonExpression.GT) subExpression).getLeft(),
                                                    ((ComparisonExpression.GT) subExpression).getRight());
            } else if (subExpression instanceof ComparisonExpression.LTE) {
                // Turn '<= into '>'
                return new ComparisonExpression.GT(((ComparisonExpression.LTE) subExpression).getLeft(),
                                                   ((ComparisonExpression.LTE) subExpression).getRight());
            } else if (subExpression instanceof ComparisonExpression.GTE) {
                // Turn '>= into '<'
                return new ComparisonExpression.LT(((ComparisonExpression.GTE) subExpression).getLeft(),
                                                   ((ComparisonExpression.GTE) subExpression).getRight());
            }

            return expression;
        }

        private IExpression simplifyOrExpression(LogicalExpression.OR expression) {
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
                return LiteralExpression.BooleanLiteral.of(false);
            }
            if (subExprSize == 1) {
                return expression.getOperands().get(0);
            }
            return expression;
        }

        private IExpression simplifyAndExpression(LogicalExpression logicalExpression) {
            Iterator<IExpression> subExpressionIterator = logicalExpression.getOperands().iterator();

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
                        return LiteralExpression.BooleanLiteral.of(false);
                    }
                } else if (IDataType.BOOLEAN.equals(subExpression.getDataType())) {
                    if (((LiteralExpression) subExpression).asBoolean()) {
                        // sub expression is true, remove it from the AND expression
                        subExpressionIterator.remove();
                    } else {
                        // The sub expression is false, the whole expression is FALSE
                        return LiteralExpression.BooleanLiteral.of(false);
                    }
                }
            }

            int subExprSize = logicalExpression.getOperands().size();
            if (subExprSize == 0) {
                return LiteralExpression.BooleanLiteral.of(true);
            }
            if (subExprSize == 1) {
                return logicalExpression.getOperands().get(0);
            }

            return logicalExpression;
        }
    }

    static class ConstantFunctionOptimizer extends AbstractOptimizer {
        // Make sure all now() function in the expression return the same value
        private LiteralExpression now;

        @Override
        public IExpression visit(FunctionExpression expression) {
            if (expression.getFunction() instanceof TimeFunction.Now) {
                if (now == null) {
                    now = LiteralExpression.LongLiteral.of((long) expression.getFunction().evaluate(null));
                }
                return now;
            }
            return super.visit(expression);
        }
    }
}
