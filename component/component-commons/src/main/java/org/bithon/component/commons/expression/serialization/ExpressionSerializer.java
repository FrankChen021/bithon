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

import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.utils.StringUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/8/18 21:08
 */
public class ExpressionSerializer {
    protected final StringBuilder sb = new StringBuilder(512);
    protected final IdentifierQuotaStrategy quoteIdentifier;
    private final String qualifier;

    public ExpressionSerializer(IdentifierQuotaStrategy quoteIdentifier) {
        this(null, quoteIdentifier);
    }

    /**
     * @param qualifier       the qualifier of the identifier if the identifier is not a qualified name
     * @param quoteIdentifier if the identifier should be quoted
     */
    public ExpressionSerializer(String qualifier, IdentifierQuotaStrategy quoteIdentifier) {
        this.qualifier = qualifier == null ? null : qualifier.trim();
        this.quoteIdentifier = quoteIdentifier;
    }

    public String serialize(IExpression expression) {
        if (expression != null) {
            expression.serializeToText(this);
        }
        return sb.toString();
    }

    public final void append(int index) {
        sb.append(index);
    }

    public final void append(char c) {
        sb.append(c);
    }

    public final void append(String str) {
        sb.append(str);
    }

    public final String getSerializedText() {
        return sb.toString();
    }

    public void serialize(LiteralExpression<?> expression) {
        Object value = expression.getValue();
        if (expression instanceof LiteralExpression.StringLiteral) {
            sb.append('\'');
            sb.append(value);
            sb.append('\'');
        } else {
            sb.append(value);
        }
    }

    public void serialize(LogicalExpression expression) {
        boolean needClose = false;
        String concatOperator = expression.getOperator();
        if (expression instanceof LogicalExpression.NOT) {
            sb.append("NOT ");
            concatOperator = "AND";
            needClose = expression.getOperands().size() > 1;
            if (needClose) {
                sb.append('(');
            }
        }
        for (int i = 0, size = expression.getOperands().size(); i < size; i++) {
            if (i > 0) {
                sb.append(' ');
                sb.append(concatOperator);
                sb.append(' ');
            }
            IExpression operand = expression.getOperands().get(i);
            if (operand instanceof ConditionalExpression || operand instanceof LogicalExpression) {
                sb.append('(');
                operand.serializeToText(this);
                sb.append(')');
            } else {
                // Might be FunctionExpression
                operand.serializeToText(this);
            }
        }
        if (needClose) {
            sb.append(')');
        }
    }

    public void serialize(IdentifierExpression expression) {
        if (StringUtils.hasText(qualifier)
            && !expression.isQualified()) {
            quoteIdentifierIfNeeded(qualifier);
            sb.append('.');
            quoteIdentifierIfNeeded(expression.getIdentifier());
        } else {
            if (expression.isQualified()) {
                quoteIdentifierIfNeeded(expression.getQualifier());
                sb.append('.');
            }
            quoteIdentifierIfNeeded(expression.getIdentifier());
        }
    }

    protected void quoteIdentifierIfNeeded(String name) {
        if (quoteIdentifier != null) {
            sb.append(quoteIdentifier.quoteIdentifier(name));
        } else {
            sb.append(name);
        }
    }

    public void serialize(FunctionExpression expression) {
        boolean first = true;
        sb.append(expression.getName());
        sb.append('(');
        for (IExpression p : expression.getArgs()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            p.serializeToText(this);
        }
        sb.append(')');
    }

    public void serialize(MacroExpression expression) {
        sb.append('{');
        sb.append(expression.getMacro());
        sb.append('}');
    }

    public void serialize(BinaryExpression expression) {
        IExpression left = expression.getLhs();
        if (left instanceof BinaryExpression) {
            sb.append('(');
        }
        left.serializeToText(this);
        if (left instanceof BinaryExpression) {
            sb.append(')');
        }
        sb.append(' ');
        sb.append(expression.getType());
        sb.append(' ');
        IExpression right = expression.getRhs();
        if (right instanceof BinaryExpression) {
            sb.append('(');
        }
        right.serializeToText(this);
        if (right instanceof BinaryExpression) {
            sb.append(')');
        }
    }
}
