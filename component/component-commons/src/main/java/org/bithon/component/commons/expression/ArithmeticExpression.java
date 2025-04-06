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

import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.component.commons.utils.StringUtils;

import java.sql.Timestamp;

/**
 * @author Frank Chen
 * @date 30/6/23 6:14 pm
 */
public abstract class ArithmeticExpression extends BinaryExpression {

    public enum ArithmeticOperator {
        ADD("+", true, 0) {
            @Override
            public IExpression newArithmeticExpression(IExpression lhs, IExpression rhs) {
                return new ADD(lhs, rhs);
            }
        },
        SUB("-", false, 0) {
            @Override
            public IExpression newArithmeticExpression(IExpression lhs, IExpression rhs) {
                return new SUB(lhs, rhs);
            }
        },
        MUL("*", true, 1) {
            @Override
            public IExpression newArithmeticExpression(IExpression lhs, IExpression rhs) {
                return new MUL(lhs, rhs);
            }
        },
        DIV("/", false, 1) {
            @Override
            public IExpression newArithmeticExpression(IExpression lhs, IExpression rhs) {
                return new DIV(lhs, rhs);
            }
        };

        private final String symbol;
        private final boolean associative;

        /**
         * it’s the value that does nothing to the result of the operation.
         * For example, 0 is the identity for addition and 1 is the identity for multiplication.
         */
        private final long identity;

        ArithmeticOperator(String symbol,
                           boolean associative,
                           long identity) {
            this.symbol = symbol;
            this.associative = associative;
            this.identity = identity;
        }

        public boolean isAssociative() {
            return associative;
        }

        public long identity() {
            return identity;
        }

        public abstract IExpression newArithmeticExpression(IExpression lhs, IExpression rhs);

        @Override
        public String toString() {
            return symbol;
        }
    }

    protected ArithmeticExpression(String operator, IExpression lhs, IExpression rhs) {
        super(operator, lhs, rhs);
    }

    /// An operator op is associative if:
    ///
    ///   (a op b) op c == a op (b op c)
    ///
    /// (1 + 2) + 3 == 1 + (2 + 3) == 6, ADD is associative, so is MUL
    /// (5 - 2) - 1 = 2 ≠ 5 - (2 - 1) = 4, SUB is not associative, so is DIV
    public abstract ArithmeticOperator getOperator();

    @Override
    public IDataType getDataType() {
        IDataType lhsType = lhs.getDataType();
        IDataType rhsType = rhs.getDataType();
        if (lhsType.equals(IDataType.STRING)) {
            return IDataType.STRING;
        }
        if (lhsType.equals(IDataType.DOUBLE) || rhsType.equals(IDataType.DOUBLE)) {
            return IDataType.DOUBLE;
        }
        return IDataType.LONG;
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        Object lhsValue = lhs.evaluate(context);
        Object rhsValue = rhs.evaluate(context);

        if (lhsValue instanceof Number) {
            Number rValue = asNumber(rhsValue);
            if (lhsValue instanceof Double
                || lhsValue instanceof HumanReadablePercentage
                || rhsValue instanceof Double
                || rhsValue instanceof HumanReadablePercentage
            ) {
                return evaluate(((Number) lhsValue).doubleValue(), rValue.doubleValue());
            }
            return evaluate(((Number) lhsValue).longValue(), rValue.longValue());
        }
        if (lhsValue instanceof String) {
            return lhsValue + rhs.evaluate(context).toString();
        }

        throw new UnsupportedOperationException(StringUtils.format("Not support '+' on type of %s and %s",
                                                                   lhsValue.getClass().getSimpleName(),
                                                                   rhsValue.getClass().getSimpleName()));
    }

    public Number evaluate(Number lhsValue, Number rhsValue) {
        if (lhsValue instanceof Double || rhsValue instanceof Double) {
            return evaluate(lhsValue.doubleValue(), rhsValue.doubleValue());
        }
        return evaluate(lhsValue.longValue(), rhsValue.longValue());
    }

    @Override
    public void accept(IExpressionInDepthVisitor visitor) {
        if (visitor.visit(this)) {
            lhs.accept(visitor);
            rhs.accept(visitor);
        }
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    private Number asNumber(Object val) {
        if (val instanceof Number) {
            return (Number) val;
        }
        if (val instanceof Timestamp) {
            return ((Timestamp) val).getTime();
        }
        throw new RuntimeException("Unable cast to number from type: " + val.getClass().getName());
    }

    protected abstract double evaluate(double v1, double v2);

    protected abstract long evaluate(long v1, long v2);

    public static class ADD extends ArithmeticExpression {
        public ADD(IExpression lhs, IExpression rhs) {
            super("+", lhs, rhs);
        }

        @Override
        public ArithmeticOperator getOperator() {
            return ArithmeticOperator.ADD;
        }

        @Override
        protected double evaluate(double v1, double v2) {
            return v1 + v2;
        }

        @Override
        protected long evaluate(long v1, long v2) {
            return v1 + v2;
        }
    }

    public static class SUB extends ArithmeticExpression {
        public SUB(IExpression lhs, IExpression rhs) {
            super("-", lhs, rhs);
        }

        @Override
        public ArithmeticOperator getOperator() {
            return ArithmeticOperator.SUB;
        }

        @Override
        protected double evaluate(double v1, double v2) {
            return v1 - v2;
        }

        @Override
        protected long evaluate(long v1, long v2) {
            return v1 - v2;
        }
    }

    public static class MUL extends ArithmeticExpression {
        public MUL(IExpression lhs, IExpression rhs) {
            super("*", lhs, rhs);
        }

        @Override
        public ArithmeticOperator getOperator() {
            return ArithmeticOperator.MUL;
        }

        @Override
        protected double evaluate(double v1, double v2) {
            return v1 * v2;
        }

        @Override
        protected long evaluate(long v1, long v2) {
            return v1 * v2;
        }
    }

    public static class DIV extends ArithmeticExpression {
        public DIV(IExpression lhs, IExpression rhs) {
            super("/", lhs, validNotZero(rhs));
        }

        private static IExpression validNotZero(IExpression rhs) {
            if (rhs instanceof LiteralExpression) {
                Object value = ((LiteralExpression<?>) rhs).getValue();
                if (value instanceof Number && ((Number) value).doubleValue() == 0) {
                    throw new ArithmeticException("Divisor CAN'T be ZERO");
                }
            }
            return rhs;
        }

        @Override
        protected double evaluate(double v1, double v2) {
            return v1 / v2;
        }

        @Override
        protected long evaluate(long v1, long v2) {
            return v1 / v2;
        }

        @Override
        public ArithmeticOperator getOperator() {
            return ArithmeticOperator.DIV;
        }

        @Override
        public IDataType getDataType() {
            return IDataType.DOUBLE;
        }
    }
}

