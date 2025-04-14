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
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.expression.MapAccessExpression;
import org.bithon.component.commons.expression.TernaryExpression;

import java.util.List;
import java.util.ListIterator;

/**
 * @author frank.chen021@outlook.com
 * @date 6/4/25 9:37 pm
 */
public class AbstractOptimizer implements IExpressionVisitor<IExpression> {
    @Override
    public IExpression visit(LiteralExpression<?> expression) {
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
        expression.setLhs(expression.getLhs().accept(this));
        expression.setRhs(expression.getRhs().accept(this));
        return expression;
    }

    @Override
    public IExpression visit(ConditionalExpression expression) {
        expression.setLhs(expression.getLhs().accept(this));
        expression.setRhs(expression.getRhs().accept(this));
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
            if (((LiteralExpression<?>) (expression.getConditionExpression())).asBoolean()) {
                return expression.getTrueExpression();
            } else {
                return expression.getFalseExpression();
            }
        }
        return expression;
    }

    protected IExpression noop(IExpression expression) {
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
