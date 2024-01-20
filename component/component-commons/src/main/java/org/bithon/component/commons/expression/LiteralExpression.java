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

import java.math.BigDecimal;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/7 20:17
 */
public abstract class LiteralExpression implements IExpression {
    protected final Object value;

    protected LiteralExpression(Object value) {
        this.value = value;
    }

    public static LiteralExpression create(Object value) {
        if (value instanceof String) {
            return new StringLiteral((String) value);
        } else if (value instanceof Long || value instanceof Integer) {
            return new LongLiteral(((Number) value).longValue());
        } else if (value instanceof Boolean) {
            return new BooleanLiteral((boolean) value);
        } else if (value instanceof Double || value instanceof Float || value instanceof BigDecimal) {
            return new DoubleLiteral((Number) value);
        } else {
            throw new UnsupportedOperationException("Not support literal type: " + value.getClass().getName());
        }
    }

    public Object getValue() {
        return value;
    }

    public boolean isNumber() {
        IDataType dataType = getDataType();
        return getDataType().equals(IDataType.DOUBLE) || dataType.equals(IDataType.LONG);
    }

    public String asString() {
        return (String) value;
    }

    public Object[] asArray() {
        return (Object[]) value;
    }

    public boolean asBoolean() {
        IDataType dataType = getDataType();
        if (dataType.equals(IDataType.BOOLEAN)) {
            return (boolean) value;
        }
        if (dataType.equals(IDataType.LONG)) {
            return ((long) value) != 0;
        }
        if (dataType.equals(IDataType.DOUBLE)) {
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

    public static class StringLiteral extends LiteralExpression {
        public StringLiteral(String value) {
            super(value);
        }

        @Override
        public IDataType getDataType() {
            return IDataType.STRING;
        }
    }

    public static class LongLiteral extends LiteralExpression {

        public LongLiteral(long value) {
            super(value);
        }

        @Override
        public IDataType getDataType() {
            return IDataType.LONG;
        }
    }

    public static class DoubleLiteral extends LiteralExpression {

        public DoubleLiteral(Number value) {
            super(value);
        }

        @Override
        public IDataType getDataType() {
            return IDataType.DOUBLE;
        }
    }

    public static class BooleanLiteral extends LiteralExpression {
        public BooleanLiteral(boolean value) {
            super(value);
        }

        @Override
        public IDataType getDataType() {
            return IDataType.BOOLEAN;
        }
    }

    public static class DateTimeLiteral extends LiteralExpression {

        public DateTimeLiteral(String value) {
            super(value);
        }

        @Override
        public IDataType getDataType() {
            return IDataType.DATETIME;
        }
    }
}
