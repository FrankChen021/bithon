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

import org.bithon.component.commons.expression.validation.ExpressionValidationException;
import org.bithon.component.commons.time.DateTime;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/7 20:17
 */
public abstract class LiteralExpression implements IExpression {

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

    protected final Object value;

    public abstract LiteralExpression castTo(IDataType targetType);

    protected LiteralExpression(Object value) {
        this.value = value;
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

        @Override
        public LiteralExpression castTo(IDataType targetType) {
            try {
                switch (targetType) {
                    case LONG:
                        return new LiteralExpression.LongLiteral(Long.parseLong(value.toString()));

                    case DOUBLE:
                        return new LiteralExpression.DoubleLiteral(Double.parseDouble(value.toString()));

                    case BOOLEAN:
                        return new LiteralExpression.BooleanLiteral("true".equalsIgnoreCase(value.toString()));
                    case STRING:
                        return this;

                    case DATETIME: {
                        try {
                            long timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value.toString())
                                                                                        .getTime();

                            return new LiteralExpression.DateTimeLiteral(DateTime.toISO8601(timestamp));
                        } catch (ParseException e) {
                            throw new ExpressionValidationException(e.getMessage());
                        }
                    }

                    default:
                        throw new UnsupportedOperationException("Can't cast a boolean value into type of " + targetType);
                }
            } catch (NumberFormatException e) {
                throw new ExpressionValidationException("Unable to parse String[%s] into number", value);
            }
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

        @Override
        public LiteralExpression castTo(IDataType targetType) {
            switch (targetType) {
                case STRING:
                    return new LiteralExpression.StringLiteral(value.toString());

                case LONG:
                    return this;

                case DOUBLE:
                    return new LiteralExpression.DoubleLiteral((Number) value);

                case BOOLEAN:
                    return new LiteralExpression.BooleanLiteral(((Number) value).longValue() != 0);

                case DATETIME:
                    return new LiteralExpression.DateTimeLiteral(DateTime.toISO8601((long) value));

                default:
                    throw new UnsupportedOperationException("Can't cast a boolean value into type of " + targetType);
            }
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

        @Override
        public LiteralExpression castTo(IDataType targetType) {
            switch (targetType) {
                case STRING:
                    return new LiteralExpression.StringLiteral(value.toString());

                case LONG:
                    return new LiteralExpression.LongLiteral(((Number) value).longValue());

                case DOUBLE:
                    return this;

                case BOOLEAN:
                    return new LiteralExpression.BooleanLiteral(((Number) value).doubleValue() != 0);

                default:
                    throw new UnsupportedOperationException("Can't cast a boolean value into type of " + targetType);
            }
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

        @Override
        public LiteralExpression castTo(IDataType targetType) {
            switch (targetType) {
                case STRING:
                    return new LiteralExpression.StringLiteral((boolean) value ? "true" : "false");

                case LONG:
                    return new LiteralExpression.LongLiteral((boolean) value ? 1 : 0);

                case BOOLEAN:
                    return this;

                default:
                    throw new UnsupportedOperationException("Can't cast a boolean value into type of " + targetType);
            }
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

        @Override
        public LiteralExpression castTo(IDataType targetType) {
            throw new UnsupportedOperationException();
        }
    }
}
