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

import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/7 20:17
 */
public abstract class BinaryExpression implements IExpression {

    /**
     * Don't change these property names because they're used in manual deserializer
     */
    protected final String type;
    protected IExpression left;
    protected IExpression right;

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

    public void setLeft(IExpression left) {
        this.left = left;
    }

    public void setRight(IExpression right) {
        this.right = right;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void accept(IExpressionVisitor visitor) {
        if (visitor.visit(this)) {
            this.left.accept(visitor);
            this.right.accept(visitor);
        }
    }

    @Override
    public <T> T accept(IExpressionVisitor2<T> visitor) {
        return visitor.visit(this);
    }

    public static class In extends BinaryExpression {

        public In(IExpression left, ExpressionList right) {
            this("in", left, right);
        }

        protected In(String operator, IExpression left, ExpressionList right) {
            super(operator, left, right);
        }

        @Override
        public IDataType getDataType() {
            return IDataType.BOOLEAN;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object evaluate(IEvaluationContext context) {
            Object l = left.evaluate(context);
            if (l == null) {
                return false;
            }

            Set<Object> sets = (Set<Object>) right.evaluate(context);
            Object o = sets.iterator().next();
            if (o instanceof String) {
                return sets.contains(l.toString());
            }
            if (o instanceof Long) {
                return sets.contains(toLong(l));
            }
            if (o instanceof Double) {
                return sets.contains(toDouble(l));
            }

            throw new UnsupportedOperationException("Type of " + o.getClass().getSimpleName() + " is not supported by IN operator");
        }

        private Object toLong(Object o) {
            if (o instanceof Long) {
                return o;
            }
            if (o instanceof Number) {
                return ((Number) o).longValue();
            }
            return Long.parseLong(o.toString());
        }

        private Object toDouble(Object o) {
            if (o instanceof Double) {
                return o;
            }
            if (o instanceof Number) {
                return ((Number) o).doubleValue();
            }
            return Double.parseDouble(o.toString());
        }
    }

    public static class Like extends BinaryExpression {

        public Like(IExpression left, IExpression right) {
            this("like", left, right);
        }

        public Like(String operator, IExpression left, IExpression right) {
            super(operator, left, right);
        }

        @Override
        public IDataType getDataType() {
            return IDataType.BOOLEAN;
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            String r = (String) right.evaluate(context);

            // Escape any special characters in the pattern
            String pattern = r.replaceAll("%", "\\\\%").replaceAll("_", "\\\\_");

            // Replace SQL wildcard characters with Java regex wildcard characters
            pattern = pattern.replaceAll("%", ".*").replaceAll("_", ".");

            String l = (String) left.evaluate(context);
            return l != null && l.contains(pattern);
        }
    }

    public static class NotLike extends Like {

        public NotLike(IExpression left, IExpression right) {
            super("not like", left, right);
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            return !((boolean) super.evaluate(context));
        }
    }

    public static class NotIn extends In {

        public NotIn(IExpression left, ExpressionList right) {
            super("not in", left, right);
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            return !((boolean) super.evaluate(context));
        }
    }
}
