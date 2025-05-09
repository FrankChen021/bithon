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

import org.bithon.component.commons.expression.serialization.ExpressionSerializer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Frank Chen
 * @date 7/8/23 2:05 pm
 */
public class ExpressionList implements IExpression {
    private final List<IExpression> expressions;

    public ExpressionList(IExpression... expressions) {
        this(Arrays.asList(expressions));
    }

    public ExpressionList(List<IExpression> expressions) {
        this.expressions = expressions;
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
        Set<Object> values = new HashSet<>();
        for (IExpression expression : expressions) {
            values.add(expression.evaluate(context));
        }
        return values;
    }

    @Override
    public void accept(IExpressionInDepthVisitor visitor) {
        if (visitor.visit(this)) {
            for (IExpression subExpression : expressions) {
                subExpression.accept(visitor);
            }
        }
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void serializeToText(ExpressionSerializer serializer) {
        serializer.append('(');
        {
            for (int i = 0, size = expressions.size(); i < size; i++) {
                if (i > 0) {
                    serializer.append(", ");
                }
                expressions.get(i).serializeToText(serializer);
            }
        }
        serializer.append(')');
    }
}
