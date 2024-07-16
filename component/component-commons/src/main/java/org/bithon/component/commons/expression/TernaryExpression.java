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
 * @date 16/7/24 3:36 pm
 */
public class TernaryExpression implements IExpression {
    private IExpression conditionExpression;
    private IExpression trueExpression;
    private IExpression falseExpression;

    public TernaryExpression(IExpression conditionExpression,
                             IExpression trueExpression,
                             IExpression falseExpression) {
        this.conditionExpression = conditionExpression;
        this.trueExpression = trueExpression;
        this.falseExpression = falseExpression;
    }

    public IExpression getConditionExpression() {
        return conditionExpression;
    }

    public IExpression getTrueExpression() {
        return trueExpression;
    }

    public IExpression getFalseExpression() {
        return falseExpression;
    }

    public void setConditionExpression(IExpression conditionExpression) {
        this.conditionExpression = conditionExpression;
    }

    public void setTrueExpression(IExpression trueExpression) {
        this.trueExpression = trueExpression;
    }

    public void setFalseExpression(IExpression falseExpression) {
        this.falseExpression = falseExpression;
    }

    @Override
    public IDataType getDataType() {
        return trueExpression.getDataType();
    }

    @Override
    public String getType() {
        return "ternary";
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        if (LogicalExpression.toBoolean(this.conditionExpression.evaluate(context))) {
            return trueExpression.evaluate(context);
        } else {
            return falseExpression.evaluate(context);
        }
    }

    @Override
    public void accept(IExpressionVisitor visitor) {
        if (visitor.visit(this)) {
            conditionExpression.accept(visitor);
            trueExpression.accept(visitor);
            falseExpression.accept(visitor);
        }
    }

    @Override
    public <T> T accept(IExpressionVisitor2<T> visitor) {
        return visitor.visit(this);
    }
}
