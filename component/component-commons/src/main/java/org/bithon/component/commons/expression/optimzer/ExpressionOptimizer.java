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

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.function.builtin.TimeFunction;

import java.util.Iterator;

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
                if (((LiteralExpression<?>) subExpression).isNumber()) {
                    if (((LiteralExpression<?>) subExpression).asBoolean()) {
                        // the sub expression is true, the whole expression is false
                        return LiteralExpression.ofBoolean(false);
                    } else {
                        return LiteralExpression.ofBoolean(true);
                    }
                } else if (IDataType.BOOLEAN.equals(subExpression.getDataType())) {
                    if (((LiteralExpression<?>) subExpression).asBoolean()) {
                        // the sub expression is true, the whole expression is false
                        return LiteralExpression.ofBoolean(false);
                    } else {
                        return LiteralExpression.ofBoolean(true);
                    }
                }
            } else if (subExpression instanceof ConditionalExpression.NotIn) {
                // Turn the expression: 'NOT var not in ('xxx')' into 'var in (xxx)'
                return new ConditionalExpression.In(((ConditionalExpression.NotIn) subExpression).getLhs(),
                                                    (ExpressionList) ((ConditionalExpression.NotIn) subExpression).getRhs());
            } else if (subExpression instanceof ConditionalExpression.In) {
                // Turn into In into NotIn
                return new ConditionalExpression.NotIn(((ConditionalExpression.In) subExpression).getLhs(),
                                                       (ExpressionList) ((ConditionalExpression.In) subExpression).getRhs());
            } else if (subExpression instanceof ComparisonExpression.EQ) {
                // Turn '=' into '<>'
                return new ComparisonExpression.NE(((ComparisonExpression.EQ) subExpression).getLhs(),
                                                   ((ComparisonExpression.EQ) subExpression).getRhs());
            } else if (subExpression instanceof ComparisonExpression.NE) {
                // Turn '<>' into '='
                return new ComparisonExpression.EQ(((ComparisonExpression.NE) subExpression).getLhs(),
                                                   ((ComparisonExpression.NE) subExpression).getRhs());
            } else if (subExpression instanceof ComparisonExpression.LT) {
                // Turn '<' into '>='
                return new ComparisonExpression.GTE(((ComparisonExpression.LT) subExpression).getLhs(),
                                                    ((ComparisonExpression.LT) subExpression).getRhs());
            } else if (subExpression instanceof ComparisonExpression.GT) {
                // Turn '>' into '<='
                return new ComparisonExpression.LTE(((ComparisonExpression.GT) subExpression).getLhs(),
                                                    ((ComparisonExpression.GT) subExpression).getRhs());
            } else if (subExpression instanceof ComparisonExpression.LTE) {
                // Turn '<= into '>'
                return new ComparisonExpression.GT(((ComparisonExpression.LTE) subExpression).getLhs(),
                                                   ((ComparisonExpression.LTE) subExpression).getRhs());
            } else if (subExpression instanceof ComparisonExpression.GTE) {
                // Turn '>= into '<'
                return new ComparisonExpression.LT(((ComparisonExpression.GTE) subExpression).getLhs(),
                                                   ((ComparisonExpression.GTE) subExpression).getRhs());
            } else if (subExpression instanceof LogicalExpression.NOT) {
                // NOT (NOT (xxx))  => xxx
                return ((LogicalExpression.NOT) subExpression).getOperands().get(0);
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

                if (((LiteralExpression<?>) subExpression).isNumber()) {
                    if (((LiteralExpression<?>) subExpression).asBoolean()) {
                        // the sub expression is true, the whole expression is true
                        return LiteralExpression.ofBoolean(true);
                    } else {
                        // The sub expression is false, it should be removed from the expression list
                        subExpressionIterator.remove();

                    }
                } else if (IDataType.BOOLEAN.equals(subExpression.getDataType())) {
                    if (((LiteralExpression<?>) subExpression).asBoolean()) {
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
                return LiteralExpression.ofBoolean(false);
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

                if (((LiteralExpression<?>) subExpression).isNumber()) {
                    if (((LiteralExpression<?>) subExpression).asBoolean()) {
                        // true, remove this always true expression
                        subExpressionIterator.remove();
                    } else {
                        // The sub expression is false, the whole expression is FALSE
                        return LiteralExpression.ofBoolean(false);
                    }
                } else if (IDataType.BOOLEAN.equals(subExpression.getDataType())) {
                    if (((LiteralExpression<?>) subExpression).asBoolean()) {
                        // sub expression is true, remove it from the AND expression
                        subExpressionIterator.remove();
                    } else {
                        // The sub expression is false, the whole expression is FALSE
                        return LiteralExpression.ofBoolean(false);
                    }
                }
            }

            int subExprSize = logicalExpression.getOperands().size();
            if (subExprSize == 0) {
                return LiteralExpression.ofBoolean(true);
            }
            if (subExprSize == 1) {
                return logicalExpression.getOperands().get(0);
            }

            return logicalExpression;
        }
    }

    static class ConstantFunctionOptimizer extends AbstractOptimizer {
        // Make sure all now() function in the expression return the same value
        private LiteralExpression.LongLiteral now;

        @Override
        public IExpression visit(FunctionExpression expression) {
            if (expression.getFunction() instanceof TimeFunction.Now) {
                if (now == null) {
                    now = LiteralExpression.ofLong((long) expression.getFunction().evaluate(null));
                }
                return now;
            }
            return super.visit(expression);
        }
    }
}
