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

import java.sql.Timestamp;

/**
 * @author Frank Chen
 * @date 30/6/23 6:14 pm
 */
public abstract class ArithmeticExpression extends BinaryExpression {

    protected ArithmeticExpression(String operator, IExpression left, IExpression right) {
        super(operator, left, right);
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        Number r1 = asNumber(left.evaluate(context));
        Number r2 = asNumber(right.evaluate(context));
        if (r1 instanceof Double || r2 instanceof Double) {
            return evaluate(r1.doubleValue(), r2.doubleValue());
        }
        return evaluate(r1.longValue(), r2.longValue());
    }

    @Override
    public void accept(IExpressionVisitor visitor) {
        if (visitor.visit(this)) {
            left.accept(visitor);
            right.accept(visitor);
        }
    }

    @Override
    public <T> T accept(IExpressionVisitor2<T> visitor) {
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
        public ADD(IExpression left, IExpression right) {
            super("+", left, right);
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
        public SUB(IExpression left, IExpression right) {
            super("-", left, right);
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
        public MUL(IExpression left, IExpression right) {
            super("*", left, right);
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
        public DIV(IExpression left, IExpression right) {
            super("/", left, right);
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
