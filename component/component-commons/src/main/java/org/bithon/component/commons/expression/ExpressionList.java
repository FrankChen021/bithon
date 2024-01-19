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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 7/8/23 2:05 pm
 */
public class ExpressionList implements IExpression {
    private final List<IExpression> expressions;
    private final Set<Object> values;

    public ExpressionList(IExpression... expressions) {
        this(Arrays.asList(expressions));
    }

    public ExpressionList(List<IExpression> expressions) {
        this.expressions = expressions;

        // Only literal is supported now
        this.values = this.expressions.stream().map((element) -> ((LiteralExpression) element).getValue()).collect(Collectors.toSet());
    }

    public List<IExpression> getExpressions() {
        return expressions;
    }

    @Override
    public IDataType getDataType() {
        return this.expressions.get(0).getDataType();
    }

    @Override
    public String getType() {
        return "()";
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        return values;
    }

    @Override
    public void accept(IExpressionVisitor visitor) {
        if (visitor.visit(this)) {
            for (IExpression subExpression : expressions) {
                subExpression.accept(visitor);
            }
        }
    }

    @Override
    public <T> T accept(IExpressionVisitor2<T> visitor) {
        return visitor.visit(this);
    }
}
