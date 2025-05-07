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
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 6/4/25 9:38 pm
 */
public class ConstantFoldingOptimizer extends AbstractOptimizer {
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
            return LiteralExpression.of(expression.evaluate(null));
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
            return LiteralExpression.of(expression.evaluate(null));
        }
        return expression;
    }

    @Override
    public IExpression visit(ArrayAccessExpression expression) {
        IExpression arrayExpression = expression.getArray().accept(this);
        if (arrayExpression instanceof LiteralExpression) {
            Object[] array = ((LiteralExpression<?>) arrayExpression).asArray();
            if (array.length > expression.getIndex()) {
                return LiteralExpression.of(array[expression.getIndex()]);
            } else {
                throw new RuntimeException("Out of index");
            }
        }
        expression.setArray(arrayExpression);
        return expression;
    }

    @Override
    public IExpression visit(ArithmeticExpression expression) {
        // Apply optimization on both LHS and RHS first
        expression.setLhs(expression.getLhs().accept(this));
        expression.setRhs(expression.getRhs().accept(this));

        // Turn 'a - (-b)' into 'a + b', where b is a literal, for further constant folding optimization
        if (expression.getOperator() == ArithmeticExpression.ArithmeticOperator.SUB
            && expression.getRhs() instanceof LiteralExpression
            && ((LiteralExpression<?>) expression.getRhs()).canNegate()
        ) {
            expression = new ArithmeticExpression.ADD(
                expression.getLhs(),
                ((LiteralExpression<?>) expression.getRhs()).negate()
            );
        }

        return fold(expression);
    }

    @Override
    public IExpression visit(ConditionalExpression expression) {
        expression.setLhs(expression.getLhs().accept(this));
        expression.setRhs(expression.getRhs().accept(this));
        if (expression.getLhs() instanceof LiteralExpression && expression.getRhs() instanceof LiteralExpression) {
            return LiteralExpression.of(expression.evaluate(null));
        }
        return expression;
    }

    private IExpression fold(IExpression expression) {
        if (expression instanceof ArithmeticExpression) {
            ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;

            IExpression lhs = fold(arithmeticExpression.getLhs());
            IExpression rhs = fold(arithmeticExpression.getRhs());

            if (arithmeticExpression.getLhs() instanceof LiteralExpression
                && arithmeticExpression.getRhs() instanceof LiteralExpression) {
                // Both lhs and rhs are literal, do evaluation directly
                return LiteralExpression.of(expression.evaluate(null));
            }

            // Handling consecutive divisions
            IExpression optimized = foldConsecutiveDivision(arithmeticExpression);
            if (optimized != null) {
                return optimized;
            }

            if (!arithmeticExpression.getOperator().isAssociative()) {
                // For non-associative ops (-, /), only fold adjacent constants
                return arithmeticExpression.getOperator()
                                           .newExpression(lhs, rhs);
            }

            //
            // Flatten and fold for associative ops (like +, *)
            //
            Number foldedValue = arithmeticExpression.getOperator().getIdentity();

            List<IExpression> flattenList = flatten(arithmeticExpression.getOperator(), lhs, rhs);
            List<IExpression> nonConstExpressions = new ArrayList<>();
            for (IExpression expr : flattenList) {
                if (expr instanceof LiteralExpression) {
                    foldedValue = arithmeticExpression.evaluate(foldedValue, (Number) expr.evaluate(null));
                } else {
                    nonConstExpressions.add(expr);
                }
            }

            IExpression result = nonConstExpressions.isEmpty()
                                 ? LiteralExpression.of(foldedValue)
                                 : reduce(nonConstExpressions, arithmeticExpression.getOperator());

            //
            // Only if the folded expression is not the identity value, should we add it to the result
            // This helps avoid returning an expression like: sum(a) + 0
            //
            if (foldedValue instanceof Long
                && (long) foldedValue == arithmeticExpression.getOperator().getIdentity()) {
                return result;
            }

            return arithmeticExpression.getOperator()
                                       .newExpression(result, LiteralExpression.of(foldedValue));
        }

        return expression;
    }

    /**
     * raw expression: a / 4 / 8
     * ast: DIV( DIV(a/4), 8)
     * optimize to: DIV(a, 32)
     */
    private IExpression foldConsecutiveDivision(ArithmeticExpression arithmeticExpression) {
        if (arithmeticExpression.getOperator() != ArithmeticExpression.ArithmeticOperator.DIV
            || !(arithmeticExpression.getRhs() instanceof LiteralExpression)) {
            return null;
        }

        // Check if LHS is also a division with a constant divisor
        IExpression lhs = arithmeticExpression.getLhs();
        IExpression rhs = arithmeticExpression.getRhs();

        if (!(lhs instanceof ArithmeticExpression)
            || ((ArithmeticExpression) lhs).getOperator() != ArithmeticExpression.ArithmeticOperator.DIV
            || !(((ArithmeticExpression) lhs).getRhs() instanceof LiteralExpression)) {
            return null;
        }

        ArithmeticExpression lhsDivExpr = (ArithmeticExpression) lhs;
        LiteralExpression<?> divisor1 = (LiteralExpression<?>) lhsDivExpr.getRhs();
        LiteralExpression<?> divisor2 = (LiteralExpression<?>) rhs;

        // Combine the divisors by multiplying them
        Number combinedDivisor = (Number) new ArithmeticExpression.MUL(divisor1, divisor2).evaluate(null);

        return new ArithmeticExpression.DIV(
            lhsDivExpr.getLhs(),
            LiteralExpression.of(combinedDivisor)
        );
    }

    /**
     * Flatten all ArithmeticExpression expressions of the same operator
     */
    private List<IExpression> flatten(ArithmeticExpression.ArithmeticOperator operator,
                                      IExpression... nodes) {
        List<IExpression> result = new ArrayList<>();
        for (IExpression node : nodes) {
            if (node instanceof ArithmeticExpression && ((ArithmeticExpression) node).getOperator() == operator) {
                result.addAll(flatten(operator,
                                      ((ArithmeticExpression) node).getLhs(),
                                      ((ArithmeticExpression) node).getRhs()));
            } else {
                result.add(node);
            }
        }
        return result;
    }

    private IExpression reduce(List<IExpression> expressionList,
                               ArithmeticExpression.ArithmeticOperator op) {
        if (expressionList.isEmpty()) {
            return LiteralExpression.of(op.getIdentity());
        }

        IExpression result = expressionList.get(0);
        for (int i = 1; i < expressionList.size(); i++) {
            result = op.newExpression(result, expressionList.get(i));
        }
        return result;
    }
}
