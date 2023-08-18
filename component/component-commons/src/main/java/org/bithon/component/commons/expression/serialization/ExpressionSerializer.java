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

package org.bithon.component.commons.expression.serialization;

import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ArrayAccessExpression;
import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.CollectionExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/8/18 21:08
 */
public class ExpressionSerializer implements IExpressionVisitor<Void> {
    protected final StringBuilder sb = new StringBuilder(512);

    public String serialize(IExpression expression) {
        expression.accept(this);
        return sb.toString();
    }

    @Override
    public Void visit(LiteralExpression expression) {
        Object value = expression.getValue();
        if (value instanceof String) {
            sb.append('\'');
            sb.append(value);
            sb.append('\'');
        } else {
            sb.append(value);
        }
        return null;
    }

    @Override
    public Void visit(LogicalExpression expression) {
        if (expression instanceof LogicalExpression.NOT) {
            sb.append("NOT (");
            expression.getOperands().get(0).accept(this);
            sb.append(")");
            return null;
        }

        for (int i = 0, size = expression.getOperands().size(); i < size; i++) {
            if (i > 0) {
                sb.append(' ');
                sb.append(expression.getOperator());
                sb.append(' ');
            }
            expression.getOperands().get(i).accept(this);
        }
        return null;
    }

    @Override
    public Void visit(IdentifierExpression expression) {
        sb.append(expression.getIdentifier());
        return null;
    }

    @Override
    public Void visit(ComparisonExpression.EQ expression) {
        return visitBinary(expression);
    }

    @Override
    public Void visit(ComparisonExpression.GT expression) {
        return visitBinary(expression);
    }

    @Override
    public Void visit(ComparisonExpression.GTE expression) {
        return visitBinary(expression);
    }

    @Override
    public Void visit(ComparisonExpression.LT expression) {
        return visitBinary(expression);
    }

    @Override
    public Void visit(ComparisonExpression.LTE expression) {
        return visitBinary(expression);
    }

    @Override
    public Void visit(ComparisonExpression.NE expression) {
        return visitBinary(expression);
    }

    @Override
    public Void visit(ComparisonExpression.IN expression) {
        return visitBinary(expression);
    }

    @Override
    public Void visit(ComparisonExpression.LIKE expression) {
        return visitBinary(expression);
    }

    @Override
    public Void visit(ArithmeticExpression.ADD expression) {
        return visitBinary(expression);
    }

    @Override
    public Void visit(ArithmeticExpression.SUB expression) {
        return visitBinary(expression);
    }

    @Override
    public Void visit(ArithmeticExpression.MUL expression) {
        return visitBinary(expression);
    }

    @Override
    public Void visit(ArithmeticExpression.DIV expression) {
        return visitBinary(expression);
    }

    @Override
    public Void visit(CollectionExpression expression) {
        sb.append('(');
        {
            List<IExpression> expressionList = expression.getElements();
            for (int i = 0, size = expressionList.size(); i < size; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                expressionList.get(i).accept(this);
            }
        }
        sb.append(')');
        return null;
    }

    @Override
    public Void visit(FunctionExpression expression) {
        boolean first = true;
        sb.append(expression.getName());
        sb.append('(');
        for (IExpression p : expression.getParameters()) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            p.accept(this);
        }
        sb.append(')');
        return null;
    }

    @Override
    public Void visit(ArrayAccessExpression expression) {
        expression.getArray().accept(this);
        sb.append('[');
        sb.append(expression.getIndex());
        sb.append(']');
        return null;
    }

    protected Void visitBinary(BinaryExpression expression) {
        IExpression left = expression.getLeft();
        if (left instanceof BinaryExpression) {
            sb.append('(');
        }
        left.accept(this);
        if (left instanceof BinaryExpression) {
            sb.append(')');
        }
        sb.append(' ');
        sb.append(expression.getType());
        sb.append(' ');

        IExpression right = expression.getRight();
        if (right instanceof BinaryExpression) {
            sb.append('(');
        }
        right.accept(this);
        if (right instanceof BinaryExpression) {
            sb.append(')');
        }
        return null;
    }
}
