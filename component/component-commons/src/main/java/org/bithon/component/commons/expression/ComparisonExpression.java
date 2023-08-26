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

import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/7/27 19:53
 */
public abstract class ComparisonExpression extends BinaryExpression {

    protected ComparisonExpression(String type, IExpression left, IExpression right) {
        super(type, left, right);
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        Object lv = left.evaluate(context);
        Object rv = right.evaluate(context);
        if (lv == null) {
            return compareLNull(rv);
        } else if (rv == null) {
            return compareRNull(lv);
        }

        if (lv instanceof String) {
            return compare((String) lv, rv.toString());
        }

        if (lv instanceof Integer || lv instanceof Long) {
            if (rv instanceof Integer || rv instanceof Long) {
                return compare(((Number) lv).longValue(), ((Number) rv).longValue());
            }
            if (rv instanceof Float || rv instanceof Double) {
                return compare(((Number) lv).doubleValue(), ((Number) rv).doubleValue());
            }
            if (rv instanceof String) {
                try {
                    return compare(((Number) lv).longValue(), Long.parseLong((String) rv));
                } catch (NumberFormatException e) {
                    throw new RuntimeException(StringUtils.format("Can't turn %s into type of Long", rv));
                }
            }
            throw new RuntimeException(StringUtils.format("Can't turn %s[%s] into type of number", rv, rv.getClass().getName()));
        }

        if (lv instanceof Double || lv instanceof Float) {
            if (rv instanceof Number) {
                return compare((double) lv, ((Number) rv).doubleValue());
            }
            if (rv instanceof String) {
                try {
                    return compare((long) lv, Long.parseLong((String) rv));
                } catch (NumberFormatException e) {
                    throw new RuntimeException(StringUtils.format("Can't turn %s into type of Long", rv));
                }
            }
        }
        throw new RuntimeException(StringUtils.format("Can't compare [%s] with [%s]", left, right));
    }

    @Override
    public void accept(IExpressionVisitor visitor) {
        if (visitor.visit(this)) {
            left.accept(visitor);
            right.accept(visitor);
        }
    }

    @Override
    public final <T> T accept(IExpressionVisitor2<T> visitor) {
        return visitor.visit(this);
    }

    protected Object compareRNull(Object lv) {
        return false;
    }

    protected Object compareLNull(Object lv) {
        return false;
    }

    protected abstract boolean compare(String v1, String v2);

    protected abstract boolean compare(double v1, double v2);

    protected abstract boolean compare(long v1, long v2);

    public static class LT extends ComparisonExpression {

        public LT(IExpression left, IExpression right) {
            super("<", left, right);
        }

        @Override
        protected boolean compare(String v1, String v2) {
            return v1.compareTo(v2) < 0;
        }

        @Override
        protected boolean compare(double v1, double v2) {
            return v1 < v2;
        }

        @Override
        protected boolean compare(long v1, long v2) {
            return v1 < v2;
        }
    }

    public static class LTE extends ComparisonExpression {

        public LTE(IExpression left, IExpression right) {
            super("<=", left, right);
        }

        @Override
        protected boolean compare(String v1, String v2) {
            return v1.compareTo(v2) <= 0;
        }

        @Override
        protected boolean compare(double v1, double v2) {
            return v1 <= v2;
        }

        @Override
        protected boolean compare(long v1, long v2) {
            return v1 <= v2;
        }
    }

    public static class GT extends ComparisonExpression {

        public GT(IExpression left, IExpression right) {
            super(">", left, right);
        }

        @Override
        protected boolean compare(String v1, String v2) {
            return v1.compareTo(v2) > 0;
        }

        @Override
        protected boolean compare(double v1, double v2) {
            return v1 > v2;
        }

        @Override
        protected boolean compare(long v1, long v2) {
            return v1 > v2;
        }
    }

    public static class GTE extends ComparisonExpression {

        public GTE(IExpression left, IExpression right) {
            super(">=", left, right);
        }

        @Override
        protected boolean compare(String v1, String v2) {
            return v1.compareTo(v2) >= 0;
        }

        @Override
        protected boolean compare(double v1, double v2) {
            return v1 >= v2;
        }

        @Override
        protected boolean compare(long v1, long v2) {
            return v1 >= v2;
        }
    }

    public static class NE extends ComparisonExpression {

        public NE(IExpression left, IExpression right) {
            super("<>", left, right);
        }

        @Override
        protected boolean compare(String v1, String v2) {
            return v1.compareTo(v2) != 0;
        }

        @Override
        protected boolean compare(double v1, double v2) {
            return v1 != v2;
        }

        @Override
        protected boolean compare(long v1, long v2) {
            return v1 != v2;
        }

        @Override
        protected Object compareRNull(Object lv) {
            return lv != null;
        }

        @Override
        protected Object compareLNull(Object lv) {
            return lv != null;
        }
    }

    public static class EQ extends ComparisonExpression {
        public EQ(IExpression left, IExpression right) {
            super("=", left, right);
        }

        @Override
        protected boolean compare(String v1, String v2) {
            return v1.compareTo(v2) == 0;
        }

        @Override
        protected boolean compare(double v1, double v2) {
            return v1 == v2;
        }

        @Override
        protected boolean compare(long v1, long v2) {
            return v1 == v2;
        }

        @Override
        protected Object compareRNull(Object lv) {
            return lv == null;
        }

        @Override
        protected Object compareLNull(Object lv) {
            return lv == null;
        }
    }

    public static class IN extends ComparisonExpression {

        public IN(IExpression left, ExpressionList right) {
            super("in", left, right);
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

        @Override
        protected boolean compare(String v1, String v2) {
            return false;
        }

        @Override
        protected boolean compare(double v1, double v2) {
            return false;
        }

        @Override
        protected boolean compare(long v1, long v2) {
            return false;
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

    public static class LIKE extends ComparisonExpression {

        public LIKE(IExpression left, IExpression right) {
            super("like", left, right);
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

        @Override
        protected boolean compare(String v1, String v2) {
            return false;
        }

        @Override
        protected boolean compare(double v1, double v2) {
            return false;
        }

        @Override
        protected boolean compare(long v1, long v2) {
            return false;
        }
    }
}
