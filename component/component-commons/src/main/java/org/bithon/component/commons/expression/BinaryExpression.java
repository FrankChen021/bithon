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
public class BinaryExpression implements IExpression {
    private final String operator;
    private final IExpression left;
    private final IExpression right;

    public BinaryExpression(String operator, IExpression left, IExpression right) {
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

    @Override
    public Object evaluate(EvaluationContext context) {
        switch (operator) {
            case "=":
                return evaluateEqual(context);
            default:
                throw new UnsupportedOperationException("Not supported operator " + operator);
        }
    }

    private Object evaluateEqual(EvaluationContext context) {
        Object leftValue = left.evaluate(context);
        Object rightValue = right.evaluate(context);
        return leftValue.equals(rightValue);
    }
}
