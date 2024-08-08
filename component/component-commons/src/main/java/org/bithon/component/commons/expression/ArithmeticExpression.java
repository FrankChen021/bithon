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

import java.sql.Timestamp;

/**
 * @author Frank Chen
 * @date 30/6/23 6:14 pm
 */
public abstract class ArithmeticExpression extends BinaryExpression {

    protected ArithmeticExpression(String operator, IExpression lhs, IExpression rhs) {
        super(operator, lhs, rhs);
    }

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
            if (lhsValue instanceof Double || rhsValue instanceof Double) {
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
            super("/", lhs, rhs);
        }

        @Override
        protected double evaluate(double v1, double v2) {
            return v1 / v2;
        }

        @Override
        protected long evaluate(long v1, long v2) {
            return v1 / v2;
        }
    }
}

