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
 * IN/NOT IN
 * LIKE/NOT LIKE
 * Comparison: >/>=/</<=/<>/=
 *
 * @author Frank Chen
 * @date 20/1/24 10:50 pm
 */
public abstract class ConditionalExpression extends BinaryExpression {

    protected ConditionalExpression(String type, IExpression left, IExpression right) {
        super(type, left, right);
    }

    @Override
    public IDataType getDataType() {
        return IDataType.BOOLEAN;
    }

    @Override
    public void accept(IExpressionInDepthVisitor visitor) {
        if (visitor.visit(this)) {
            this.left.accept(visitor);
            this.right.accept(visitor);
        }
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public static class In extends ConditionalExpression {

        public In(IExpression left, ExpressionList right) {
            this("in", left, right);
        }

        protected In(String operator, IExpression left, ExpressionList right) {
            super(operator, left, right);
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

    public static class Like extends ConditionalExpression {

        public Like(IExpression left, IExpression right) {
            this("like", left, right);
        }

        public Like(String operator, IExpression left, IExpression right) {
            super(operator, left, right);
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            String input = (String) left.evaluate(context);
            if (input == null) {
                return false;
            }

            String pattern = (String) right.evaluate(context);

            if (pattern.contains("%")) {
                // Replace % with .* to convert SQL LIKE pattern to a regex pattern
                String regexPattern = pattern.replace("%", ".*");

                // Use regex matching to check if the inputString matches the pattern
                return input.matches(regexPattern);
            } else {
                // If the given pattern does not contain '%', check for exact match
                return input.equals(pattern);
            }
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

    public static class Contains extends ConditionalExpression {
        public Contains(IExpression left, IExpression right) {
            super("contains", left, right);
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            String input = (String) left.evaluate(context);
            if (input == null) {
                return false;
            }

            String pattern = (String) right.evaluate(context);
            return input.contains(pattern);
        }
    }

    public static class StartsWith extends ConditionalExpression {
        public StartsWith(IExpression left, IExpression right) {
            super("startsWith", left, right);
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            String input = (String) left.evaluate(context);
            if (input == null) {
                return false;
            }

            String pattern = (String) right.evaluate(context);
            return input.startsWith(pattern);
        }
    }

    public static class EndsWith extends ConditionalExpression {
        public EndsWith(IExpression left, IExpression right) {
            super("endsWith", left, right);
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            String input = (String) left.evaluate(context);
            if (input == null) {
                return false;
            }

            String pattern = (String) right.evaluate(context);
            return input.endsWith(pattern);
        }
    }

    public static class IsNull extends ConditionalExpression {

        public IsNull(IExpression left) {
            super("IS", left, LiteralExpression.NullLiteral.INSTANCE);
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            return left.evaluate(context) == null;
        }
    }
}
