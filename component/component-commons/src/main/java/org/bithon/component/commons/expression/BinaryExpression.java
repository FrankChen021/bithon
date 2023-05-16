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

import org.bithon.component.commons.utils.Preconditions;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/7 20:17
 */
public abstract class BinaryExpression implements IExpression {

    /**
     * Don't change these property names because they're used in manual deserializer
     */
    protected final String type;
    protected final IExpression left;
    protected final IExpression right;

    protected BinaryExpression(String type, IExpression left, IExpression right) {
        this.type = type;
        this.left = left;
        this.right = right;
    }

    public IExpression getLeft() {
        return left;
    }

    public IExpression getRight() {
        return right;
    }

    @Override
    public String getType() {
        return type;
    }

    /**
     * Override for debugging
     */
    @Override
    public String toString() {
        return this.left + " " + this.type + " " + this.right;
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        return matches(left.evaluate(context), right.evaluate(context));
    }

    protected boolean matches(Object lValue, Object rValue) {
        if (lValue == null || rValue == null) {
            return matchesNull(lValue, rValue);
        }

        if (lValue instanceof Number) {
            if (rValue instanceof Number) {
                return matches((Number) lValue, (Number) rValue);
            } else {
                return matches((Number) lValue, Long.parseLong(rValue.toString()));
            }
        }

        return matches(lValue.toString(), rValue.toString());
    }

    protected abstract boolean matchesNull(Object left, Object right);

    protected abstract boolean matches(Number left, Number right);

    protected abstract boolean matches(String left, String right);

    public static class EQ extends BinaryExpression {
        public EQ(IExpression left, IExpression right) {
            super("=", left, right);
        }

        protected EQ(String operator, IExpression left, IExpression right) {
            super(operator, left, right);
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

    public static class IN extends EQ {
        public IN(IExpression left, IExpression right) {
            super("in", left, right);

            Preconditions.checkIfTrue(right instanceof ExpressionList,
                                      "The 2nd expression of IN operator must be type of ExpressionList");
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            Object leftVal = left.evaluate(context);

            for (IExpression expr : ((ExpressionList) right).getExpressionList()) {
                if (this.matches(leftVal, expr.evaluate(context))) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public <T> T accept(IExpressionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LIKE extends BinaryExpression {
        public LIKE(IExpression left, IExpression right) {
            super("like", left, right);
        }

        @Override
        protected boolean matchesNull(Object left, Object right) {
            return false;
        }

        @Override
        protected boolean matches(Number left, Number right) {
            throw new UnsupportedOperationException("LIKE operator is not supported on both numbers");
        }

        @Override
        protected boolean matches(String left, String right) {
            // Escape any special characters in the pattern
            String pattern = right.replaceAll("%", "\\\\%").replaceAll("_", "\\\\_");

            // Replace SQL wildcard characters with Java regex wildcard characters
            pattern = pattern.replaceAll("%", ".*").replaceAll("_", ".");

            return left.contains(pattern);
        }

        @Override
        public <T> T accept(IExpressionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
