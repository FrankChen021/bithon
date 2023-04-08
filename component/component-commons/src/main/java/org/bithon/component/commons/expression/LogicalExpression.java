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
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * AND/OR
 *
 * @author frankchen
 */
public class LogicalExpression implements IExpression {
    private final String operator;
    private final List<IExpression> operands;
    private final Function<EvaluationContext, Object> evaluator;

    public LogicalExpression(String operator, IExpression... operands) {
        this(operator, Arrays.asList(operands));
    }

    public LogicalExpression(String operator, List<IExpression> operands) {
        this.operator = operator.toUpperCase(Locale.ENGLISH);
        this.operands = operands;

        switch (this.operator) {
            case "AND":
                this.evaluator = this::and;
                break;
            case "OR":
                this.evaluator = this::or;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported logic operator " + this.operator);
        }
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
    public Object evaluate(EvaluationContext context) {
        return this.evaluator.apply(context);
    }

    private Object and(EvaluationContext context) {
        for (IExpression expression : this.operands) {
            boolean v = toBoolean(expression.evaluate(context));
            if (!v) {
                return false;
            }
        }
        return true;
    }

    private Object or(EvaluationContext context) {
        for (IExpression expression : this.operands) {
            boolean v = toBoolean(expression.evaluate(context));
            if (v) {
                return true;
            }
        }
        return false;
    }

    private boolean toBoolean(Object value) {
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
}
