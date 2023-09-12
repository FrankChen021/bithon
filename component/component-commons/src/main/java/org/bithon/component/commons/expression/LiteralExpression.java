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

import org.bithon.component.commons.expression.function.IDataType;

import java.math.BigDecimal;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/7 20:17
 */
public class LiteralExpression implements IExpression {
    private final Object value;
    private final IDataType dataType;

    public LiteralExpression(String value) {
        this.value = value;
        this.dataType = IDataType.STRING;
    }

    public LiteralExpression(long value) {
        this.value = value;
        this.dataType = IDataType.LONG;
    }

    public LiteralExpression(double value) {
        this.value = value;
        this.dataType = IDataType.DOUBLE;
    }

    public LiteralExpression(boolean value) {
        this.value = value;
        this.dataType = IDataType.BOOLEAN;
    }

    public LiteralExpression(Object value) {
        this.value = value;
        if (value instanceof String) {
            this.dataType = IDataType.STRING;
        } else if (value instanceof Long || value instanceof Integer) {
            this.dataType = IDataType.LONG;
        } else if (value instanceof Boolean) {
            this.dataType = IDataType.BOOLEAN;
        } else if (value instanceof Double || value instanceof Float || value instanceof BigDecimal) {
            this.dataType = IDataType.DOUBLE;
        } else {
            throw new UnsupportedOperationException("Not support literal type: " + value.getClass().getName());
        }
    }

    public IDataType getDataType() {
        return this.dataType;
    }

    public Object getValue() {
        return value;
    }

    public boolean isNumber() {
        return this.dataType.equals(IDataType.DOUBLE) || this.dataType.equals(IDataType.LONG);
    }

    public String asString() {
        return (String) value;
    }

    public Object[] asArray() {
        return (Object[]) value;
    }

    public boolean asBoolean() {
        if (this.dataType.equals(IDataType.BOOLEAN)) {
            return (boolean) value;
        }
        if (this.dataType.equals(IDataType.LONG)) {
            return ((long) value) > 0;
        }
        if (this.dataType.equals(IDataType.DOUBLE)) {
            return ((double) value) != 0;
        }
        throw new RuntimeException("Unable to convert to boolean for expression: " + this.serializeToText());
    }

    @Override
    public String getType() {
        return "literal";
    }

    @Override
    public void accept(IExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> T accept(IExpressionVisitor2<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        return value;
    }
}
