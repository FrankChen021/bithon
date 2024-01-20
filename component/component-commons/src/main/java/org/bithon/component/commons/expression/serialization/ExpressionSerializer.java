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
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.expression.MapAccessExpression;
import org.bithon.component.commons.utils.StringUtils;

import java.util.List;
import java.util.function.Function;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/8/18 21:08
 */
public class ExpressionSerializer implements IExpressionVisitor {
    protected final StringBuilder sb = new StringBuilder(512);

    protected final Function<String, String> quoteIdentifier;
    private final String qualifier;

    public ExpressionSerializer(Function<String, String> quoteIdentifier) {
        this(null, quoteIdentifier);
    }

    /**
     * @param qualifier       the qualifier of the identifier if the identifier is not a qualified name
     * @param quoteIdentifier if the identifier should be quoted
     */
    public ExpressionSerializer(String qualifier, Function<String, String> quoteIdentifier) {
        this.qualifier = qualifier == null ? null : qualifier.trim();
        this.quoteIdentifier = quoteIdentifier;
    }

    public String serialize(IExpression expression) {
        expression.accept(this);
        return sb.toString();
    }

    @Override
    public boolean visit(LiteralExpression expression) {
        Object value = expression.getValue();
        if (value instanceof String) {
            sb.append('\'');
            sb.append(value);
            sb.append('\'');
        } else {
            sb.append(value);
        }
        return false;
    }

    @Override
    public boolean visit(LogicalExpression expression) {
        if (expression instanceof LogicalExpression.NOT) {
            sb.append("NOT (");
            expression.getOperands().get(0).accept(this);
            sb.append(")");
            return false;
        }

        sb.append('(');
        for (int i = 0, size = expression.getOperands().size(); i < size; i++) {
            if (i > 0) {
                sb.append(' ');
                sb.append(expression.getOperator());
                sb.append(' ');
            }
            expression.getOperands().get(i).accept(this);
        }
        sb.append(')');
        return false;
    }

    @Override
    public boolean visit(IdentifierExpression expression) {
        if (StringUtils.hasText(qualifier)
            && !expression.isQualified()) {
            quoteIdentifierIfNeeded(qualifier);
            sb.append('.');
        }
        quoteIdentifierIfNeeded(expression.getIdentifier());
        return false;
    }

    protected void quoteIdentifierIfNeeded(String name) {
        if (quoteIdentifier != null) {
            sb.append(quoteIdentifier.apply(name));
        } else {
            sb.append(name);
        }
    }

    @Override
    public boolean visit(ComparisonExpression expression) {
        visit((BinaryExpression) expression);
        return false;
    }

    @Override
    public boolean visit(ArithmeticExpression expression) {
        visit((BinaryExpression) expression);
        return false;
    }

    @Override
    public boolean visit(ExpressionList expression) {
        sb.append('(');
        {
            List<IExpression> expressionList = expression.getExpressions();
            for (int i = 0, size = expressionList.size(); i < size; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                expressionList.get(i).accept(this);
            }
        }
        sb.append(')');
        return false;
    }

    @Override
    public boolean visit(FunctionExpression expression) {
        boolean first = true;
        sb.append(expression.getName());
        sb.append('(');
        for (IExpression p : expression.getParameters()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            p.accept(this);
        }
        sb.append(')');
        return false;
    }

    @Override
    public boolean visit(ArrayAccessExpression expression) {
        expression.getArray().accept(this);
        sb.append('[');
        sb.append(expression.getIndex());
        sb.append(']');
        return false;
    }

    @Override
    public boolean visit(MapAccessExpression expression) {
        expression.getMap().accept(this);
        sb.append('[');
        sb.append('\'');
        sb.append(expression.getProp());
        sb.append('\'');
        sb.append(']');
        return false;
    }

    @Override
    public boolean visit(MacroExpression expression) {
        sb.append('{');
        sb.append(expression.getMacro());
        sb.append('}');
        return false;
    }

    @Override
    public boolean visit(BinaryExpression expression) {
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

        return false;
    }
}
