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

import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/7 20:17
 */
public abstract class BinaryExpression implements IExpression {

    public static BinaryExpression create(String operator, IExpression left, IExpression right) {
        switch (operator) {
            case "=":
                return new EQ(left, right);
            case ">":
                return new GT(left, right);
            case ">=":
                return new GTE(left, right);
            case "<":
                return new LT(left, right);
            case "<=":
                return new LTE(left, right);
            case "<>":
            case "!=":
                return new NE(left, right);
            default:
                throw new UnsupportedOperationException("Not supported operator " + operator);
        }
    }

    protected final String operator;
    protected final IExpression leftExpression;
    protected final IExpression rightExpression;

    protected BinaryExpression(String operator, IExpression leftExpression, IExpression rightExpression) {
        this.operator = operator;
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
    }

    public String getOperator() {
        return operator;
    }

    public IExpression getLeftExpression() {
        return leftExpression;
    }

    public IExpression getRightExpression() {
        return rightExpression;
    }

    @Override
    public String getType() {
        return "binary";
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        Object left = leftExpression.evaluate(context);
        Object right = rightExpression.evaluate(context);

        if (left == null || right == null) {
            return matchesNull(left, right);
        }

        if (left instanceof Number) {
            if (right instanceof Number) {
                return matches((Number) left, (Number) right);
            } else {
                return matches((Number) left, Long.parseLong(right.toString()));
            }
        }

        return matches(left.toString(), right.toString());
    }

    protected abstract boolean matchesNull(Object left, Object right);

    protected abstract boolean matches(Number left, Number right);

    protected abstract boolean matches(String left, String right);

    public static class EQ extends BinaryExpression {
        public EQ(IExpression left, IExpression right) {
            super("=", left, right);
        }

        @Override
        public <T> T accept(IExpressionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        protected boolean matchesNull(Object left, Object right) {
            return left == null && right == null;
        }

        @Override
        protected boolean matches(Number left, Number right) {
            return left.longValue() == right.longValue();
        }

        @Override
        protected boolean matches(String left, String right) {
            return left.equals(right);
        }
    }

    public static class GT extends BinaryExpression {
        public GT(IExpression left, IExpression right) {
            super(">", left, right);
        }

        @Override
        public <T> T accept(IExpressionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        protected boolean matchesNull(Object left, Object right) {
            return false;
        }

        @Override
        protected boolean matches(Number left, Number right) {
            // might be not correct if these two numbers are the type of double
            return left.longValue() > right.longValue();
        }

        @Override
        protected boolean matches(String left, String right) {
            return left.compareTo(right) > 0;
        }
    }

    public static class GTE extends BinaryExpression {
        public GTE(IExpression left, IExpression right) {
            super(">=", left, right);
        }

        @Override
        public <T> T accept(IExpressionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        protected boolean matchesNull(Object left, Object right) {
            return false;
        }

        @Override
        protected boolean matches(Number left, Number right) {
            // might be not correct if these two numbers are the type of double
            return left.longValue() >= right.longValue();
        }

        @Override
        protected boolean matches(String left, String right) {
            return left.compareTo(right) >= 0;
        }
    }

    public static class LT extends BinaryExpression {
        public LT(IExpression left, IExpression right) {
            super("<", left, right);
        }

        @Override
        public <T> T accept(IExpressionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        protected boolean matchesNull(Object left, Object right) {
            return false;
        }

        @Override
        protected boolean matches(Number left, Number right) {
            // might be not correct if these two numbers are the type of double
            return left.longValue() < right.longValue();
        }

        @Override
        protected boolean matches(String left, String right) {
            return left.compareTo(right) < 0;
        }
    }

    public static class LTE extends BinaryExpression {
        public LTE(IExpression left, IExpression right) {
            super("<=", left, right);
        }

        @Override
        public <T> T accept(IExpressionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        protected boolean matchesNull(Object left, Object right) {
            return false;
        }

        @Override
        protected boolean matches(Number left, Number right) {
            // might be not correct if these two numbers are the type of double
            return left.longValue() <= right.longValue();
        }

        @Override
        protected boolean matches(String left, String right) {
            return left.compareTo(right) <= 0;
        }
    }

    public static class NE extends BinaryExpression {
        public NE(IExpression left, IExpression right) {
            super("<>", left, right);
        }

        @Override
        public <T> T accept(IExpressionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        protected boolean matchesNull(Object left, Object right) {
            return !(left == null && right == null);
        }

        @Override
        protected boolean matches(Number left, Number right) {
            return left.longValue() != right.longValue();
        }

        @Override
        protected boolean matches(String left, String right) {
            return !left.equals(right);
        }
    }

    public static class DebuggableExpression implements IExpression {
        private static final ILogAdaptor LOG = LoggerFactory.getLogger(DebuggableExpression.class);

        private final String expression;
        private final BinaryExpression delegate;

        public DebuggableExpression(String expression, BinaryExpression delegate) {
            this.expression = expression;
            this.delegate = delegate;
        }

        @Override
        public String getType() {
            return delegate.getType();
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            boolean ret = (boolean) delegate.evaluate(context);
            LOG.info("Expression[{}] evaluates to be {}: left val={}, right val = {}",
                     expression,
                     ret,
                     delegate.leftExpression.evaluate(context),
                     delegate.rightExpression.evaluate(context));
            return ret;
        }

        @Override
        public <T> T accept(IExpressionVisitor<T> visitor) {
            return delegate.accept(visitor);
        }
    }
}
