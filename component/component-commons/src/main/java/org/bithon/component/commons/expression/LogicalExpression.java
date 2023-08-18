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

package org.bithon.component.commons.expression;

import org.bithon.component.commons.utils.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * AND/OR
 *
 * @author frankchen
 */
public abstract class LogicalExpression implements IExpression {

    public static LogicalExpression create(String operator, List<IExpression> expressions) {
        switch (operator) {
            case "AND":
                return new AND(expressions);
            case "OR":
                return new OR(expressions);
            case "NOT":
                return new NOT(expressions);
            default:
                throw new UnsupportedOperationException("Unsupported operator " + operator);
        }
    }

    protected final String operator;
    protected final List<IExpression> operands;

    protected LogicalExpression(String operator, List<IExpression> operands) {
        this.operator = operator.toUpperCase(Locale.ENGLISH);
        this.operands = operands;
    }

    public String getOperator() {
        return operator;
    }

    public List<IExpression> getOperands() {
        return operands;
    }

    @Override
    public String getType() {
        return "logical";
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void serializeToText(StringBuilder sb) {
        for (int i = 0, size = this.operands.size(); i < size; i++) {
            if (i > 0) {
                sb.append(' ');
                sb.append(this.operator);
                sb.append(' ');
            }
            this.operands.get(i).serializeToText(sb);
        }
    }

    public abstract LogicalExpression copy(List<IExpression> expressionList);

    private static boolean toBoolean(Object value) {
        if (value == null) {
            throw new RuntimeException("value is null");
        }
        if (value instanceof Boolean) {
            return (boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        if (value instanceof String) {
            return "true".equals(value);
        }
        throw new RuntimeException(StringUtils.format("value [%s] can not be cast to type of Boolean.", value));
    }

    public static class AND extends LogicalExpression {
        public AND(List<IExpression> operands) {
            super("AND", operands);
        }

        public AND(IExpression... expressions) {
            super("AND", Arrays.asList(expressions));
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            for (IExpression expression : this.operands) {
                boolean v = toBoolean(expression.evaluate(context));
                if (!v) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public LogicalExpression copy(List<IExpression> expressionList) {
            return new AND(expressionList);
        }
    }

    public static class OR extends LogicalExpression {

        public OR(List<IExpression> operands) {
            super("AND", operands);
        }

        public OR(IExpression... operands) {
            super("OR", Arrays.asList(operands));
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            for (IExpression expression : this.operands) {
                boolean v = toBoolean(expression.evaluate(context));
                if (v) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public LogicalExpression copy(List<IExpression> expressionList) {
            return new OR(expressionList);
        }
    }

    public static class NOT extends LogicalExpression {

        public NOT(List<IExpression> operands) {
            super("NOT", operands);
        }

        public NOT(IExpression expression) {
            super("NOT", Collections.singletonList(expression));
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            IExpression expression = operands.get(0);
            return !toBoolean(expression.evaluate(context));
        }

        @Override
        public LogicalExpression copy(List<IExpression> expressionList) {
            return new NOT(expressionList);
        }

        @Override
        public void serializeToText(StringBuilder sb) {
            sb.append("NOT (");
            operands.get(0).serializeToText(sb);
            sb.append(")");
        }
    }
}
