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
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.component.commons.utils.HumanReadablePercentage;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/7 20:17
 */
public abstract class LiteralExpression<T> implements IExpression {

    public static StringLiteral ofString(String value) {
        return new StringLiteral(value);
    }

    public static LongLiteral ofLong(int value) {
        return new LongLiteral(value);
    }

    public static LongLiteral ofLong(long value) {
        return new LongLiteral(value);
    }

    public static LongLiteral ofLong(String val) {
        return new LongLiteral(Long.parseLong(val));
    }

    public static BooleanLiteral ofBoolean(boolean value) {
        return value ? BooleanLiteral.TRUE : BooleanLiteral.FALSE;
    }

    public static DoubleLiteral ofDouble(double value) {
        return new DoubleLiteral(value);
    }

    public static DoubleLiteral ofDouble(float value) {
        return new DoubleLiteral(value);
    }

    public static BigDecimalLiteral ofDecimal(BigDecimal value) {
        return new BigDecimalLiteral(value);
    }

    public static ReadableDurationLiteral of(HumanReadableDuration value) {
        return new ReadableDurationLiteral(value);
    }

    public static ReadableNumberLiteral of(HumanReadableNumber value) {
        return new ReadableNumberLiteral(value);
    }

    public static ReadablePercentageLiteral of(HumanReadablePercentage value) {
        return new ReadablePercentageLiteral(value);
    }

    public static LiteralExpression<?> of(Object value) {
        if (value instanceof String) {
            return new StringLiteral((String) value);
        } else if (value instanceof Long || value instanceof Integer) {
            return new LongLiteral(((Number) value).longValue());
        } else if (value instanceof Boolean) {
            return BooleanLiteral.ofBoolean((boolean) value);
        } else if (value instanceof Double || value instanceof Float) {
            return new DoubleLiteral(((Number) value).doubleValue());
        } else if (value instanceof HumanReadableDuration) {
            return new ReadableDurationLiteral((HumanReadableDuration) value);
        } else if (value instanceof HumanReadableNumber) {
            return new ReadableNumberLiteral((HumanReadableNumber) value);
        } else if (value instanceof HumanReadablePercentage) {
            return new ReadablePercentageLiteral((HumanReadablePercentage) value);
        } else if (value instanceof BigDecimal) {
            return new BigDecimalLiteral((BigDecimal) value);
        } else if (value instanceof Number) {
            // User defined Number, treat it as DOUBLE
            return new DoubleLiteral(((Number) value).doubleValue());
        } else {
            throw new UnsupportedOperationException("Not support literal type: " + value.getClass().getName());
        }
    }

    protected final T value;

    public abstract LiteralExpression<?> castTo(IDataType targetType);

    protected LiteralExpression(T value) {
        this.value = value;
    }

    /**
     * NOTE: do not change the method name since it's used in the ExpressionDeserializer}
     */
    public T getValue() {
        return value;
    }

    public boolean isNumber() {
        IDataType dataType = getDataType();
        return getDataType().equals(IDataType.DOUBLE) || dataType.equals(IDataType.LONG);
    }

    public String asString() {
        return value.toString();
    }

    public Object[] asArray() {
        return (Object[]) value;
    }

    public abstract boolean asBoolean();

    @Override
    public String getType() {
        return "literal";
    }

    @Override
    public void accept(IExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <TT> TT accept(IExpressionVisitor2<TT> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public static class StringLiteral extends LiteralExpression<String> {
        public StringLiteral(String value) {
            super(value);
        }

        @Override
        public boolean asBoolean() {
            return "true".equals(value);
        }

        @Override
        public IDataType getDataType() {
            return IDataType.STRING;
        }

        @Override
        public LiteralExpression<?> castTo(IDataType targetType) {
            try {
                switch (targetType) {
                    case LONG:
                        return new LiteralExpression.LongLiteral(Long.parseLong(value));

                    case DOUBLE:
                        return new LiteralExpression.DoubleLiteral(Double.parseDouble(value));

                    case BOOLEAN:
                        return new LiteralExpression.BooleanLiteral("true".equalsIgnoreCase(value));

                    case STRING:
                        return this;

                    case DATETIME_3: {
                        try {
                            long timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).parse(value).getTime();

                            return new TimestampLiteral(timestamp);
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

    public static class LongLiteral extends LiteralExpression<Long> {

        public LongLiteral(long value) {
            super(value);
        }

        @Override
        public IDataType getDataType() {
            return IDataType.LONG;
        }

        @Override
        public LiteralExpression<?> castTo(IDataType targetType) {
            switch (targetType) {
                case STRING:
                    return new LiteralExpression.StringLiteral(value.toString());

                case LONG:
                    return this;

                case DOUBLE:
                    return new LiteralExpression.DoubleLiteral(((Number) value).doubleValue());

                case BOOLEAN:
                    return new LiteralExpression.BooleanLiteral(value != 0);

                case DATETIME_3:
                    return new TimestampLiteral(value);

                default:
                    throw new UnsupportedOperationException("Can't cast a boolean value into type of " + targetType);
            }
        }

        @Override
        public boolean asBoolean() {
            return value != 0;
        }
    }

    public static class DoubleLiteral extends LiteralExpression<Double> {

        public DoubleLiteral(double value) {
            super(value);
        }

        @Override
        public IDataType getDataType() {
            return IDataType.DOUBLE;
        }

        @Override
        public LiteralExpression<?> castTo(IDataType targetType) {
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

        @Override
        public boolean asBoolean() {
            return value != 0;
        }
    }

    /**
     * BigDecimal is used to represent the number in fixed point
     */
    public static class BigDecimalLiteral extends LiteralExpression<BigDecimal> {

        protected BigDecimalLiteral(BigDecimal value) {
            super(value);
        }

        @Override
        public LiteralExpression<?> castTo(IDataType targetType) {
            switch (targetType) {
                case STRING:
                    return new LiteralExpression.StringLiteral(value.toString());

                case LONG:
                    return new LiteralExpression.LongLiteral(((Number) value).longValue());

                case DOUBLE:
                    return new LiteralExpression.DoubleLiteral(value.doubleValue());

                case BOOLEAN:
                    return new LiteralExpression.BooleanLiteral(((Number) value).doubleValue() != 0);

                default:
                    throw new UnsupportedOperationException("Can't cast a boolean value into type of " + targetType);
            }
        }

        @Override
        public boolean asBoolean() {
            return this.value.compareTo(BigDecimal.ZERO) != 0;
        }

        @Override
        public IDataType getDataType() {
            return IDataType.DOUBLE;
        }
    }

    public static class BooleanLiteral extends LiteralExpression<Boolean> {
        public static final BooleanLiteral TRUE = new BooleanLiteral(true);
        public static final BooleanLiteral FALSE = new BooleanLiteral(false);

        private BooleanLiteral(boolean value) {
            super(value);
        }

        @Override
        public IDataType getDataType() {
            return IDataType.BOOLEAN;
        }

        @Override
        public boolean asBoolean() {
            return value;
        }

        public BooleanLiteral negate() {
            return value ? FALSE : TRUE;
        }

        @Override
        public LiteralExpression<?> castTo(IDataType targetType) {
            switch (targetType) {
                case STRING:
                    return new LiteralExpression.StringLiteral(value ? "true" : "false");

                case LONG:
                    return new LiteralExpression.LongLiteral(value ? 1 : 0);

                case BOOLEAN:
                    return this;

                default:
                    throw new UnsupportedOperationException("Can't cast a boolean value into type of " + targetType);
            }
        }
    }

    public static class TimestampLiteral extends LiteralExpression<Long> {

        public TimestampLiteral(long milliseconds) {
            super(milliseconds);
        }

        @Override
        public IDataType getDataType() {
            return IDataType.DATETIME_3;
        }

        @Override
        public LiteralExpression<?> castTo(IDataType targetType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean asBoolean() {
            return value != 0;
        }
    }

    public static class NullLiteral extends LiteralExpression<Void> {
        public static final NullLiteral INSTANCE = new NullLiteral();

        public NullLiteral() {
            super(null);
        }

        @Override
        public LiteralExpression<?> castTo(IDataType targetType) {
            return this;
        }

        @Override
        public boolean asBoolean() {
            return false;
        }

        @Override
        public IDataType getDataType() {
            // May not be right
            return IDataType.STRING;
        }

        @Override
        public String toString() {
            return "null";
        }
    }

    public static class AsteriskLiteral extends LiteralExpression<String> {
        public static final AsteriskLiteral INSTANCE = new AsteriskLiteral();

        public AsteriskLiteral() {
            super("*");
        }

        @Override
        public LiteralExpression<?> castTo(IDataType targetType) {
            return this;
        }

        @Override
        public boolean asBoolean() {
            throw new UnsupportedOperationException();
        }

        @Override
        public IDataType getDataType() {
            // May not be right
            return IDataType.STRING;
        }

        @Override
        public String toString() {
            return "AsteriskLiteral(*)";
        }
    }

    public static class ReadableDurationLiteral extends LiteralExpression<HumanReadableDuration> {
        public ReadableDurationLiteral(HumanReadableDuration duration) {
            super(duration);
        }

        @Override
        public boolean asBoolean() {
            throw new UnsupportedOperationException();
        }

        @Override
        public IDataType getDataType() {
            return IDataType.LONG;
        }

        @Override
        public LiteralExpression<?> castTo(IDataType targetType) {
            throw new UnsupportedOperationException();
        }
    }

    public static class ReadableNumberLiteral extends LiteralExpression<HumanReadableNumber> {
        public ReadableNumberLiteral(HumanReadableNumber size) {
            super(size);
        }

        @Override
        public boolean asBoolean() {
            return value.getValue() != 0;
        }

        @Override
        public IDataType getDataType() {
            return IDataType.LONG;
        }

        @Override
        public LiteralExpression<?> castTo(IDataType targetType) {
            throw new UnsupportedOperationException();
        }
    }

    public static class ReadablePercentageLiteral extends LiteralExpression<HumanReadablePercentage> {
        public ReadablePercentageLiteral(HumanReadablePercentage percentage) {
            super(percentage);
        }

        @Override
        public boolean asBoolean() {
            throw new UnsupportedOperationException();
        }

        @Override
        public IDataType getDataType() {
            return IDataType.DOUBLE;
        }

        @Override
        public LiteralExpression<?> castTo(IDataType targetType) {
            throw new UnsupportedOperationException();
        }
    }
}
