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
    protected final IExpression left;
    protected final IExpression right;

    protected BinaryExpression(String operator, IExpression left, IExpression right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public String getOperator() {
        return operator;
    }

    public IExpression getLeft() {
        return left;
    }

    public IExpression getRight() {
        return right;
    }

    @Override
    public String getType() {
        return "binary";
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public static class EQ extends BinaryExpression {
        public EQ(IExpression left, IExpression right) {
            super("=", left, right);
        }

        @Override
        public Object evaluate(EvaluationContext context) {
            Object leftValue = left.evaluate(context);
            Object rightValue = right.evaluate(context);
            return leftValue.equals(rightValue);
        }
    }

    public static class GT extends BinaryExpression {
        public GT(IExpression left, IExpression right) {
            super(">", left, right);
        }

        @Override
        public Object evaluate(EvaluationContext context) {
            return null;
        }
    }

    public static class GTE extends BinaryExpression {
        public GTE(IExpression left, IExpression right) {
            super(">=", left, right);
        }

        @Override
        public Object evaluate(EvaluationContext context) {
            return null;
        }
    }

    public static class LT extends BinaryExpression {
        public LT(IExpression left, IExpression right) {
            super("<", left, right);
        }

        @Override
        public Object evaluate(EvaluationContext context) {
            return null;
        }
    }

    public static class LTE extends BinaryExpression {
        public LTE(IExpression left, IExpression right) {
            super("<=", left, right);
        }

        @Override
        public Object evaluate(EvaluationContext context) {
            return null;
        }
    }

    public static class NE extends BinaryExpression {
        public NE(IExpression left, IExpression right) {
            super("<>", left, right);
        }

        @Override
        public Object evaluate(EvaluationContext context) {
            return null;
        }
    }
}
