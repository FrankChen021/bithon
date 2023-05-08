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
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 8/5/23 1:47 pm
 */
public class ExpressionList implements IExpression {
    private final List<IExpression> expressionList;

    public ExpressionList(IExpression... expressions) {
        this(Arrays.asList(expressions));
    }

    public ExpressionList(List<IExpression> expressions) {
        this.expressionList = expressions;
    }

    @Override
    public String getType() {
        return "expressionList";
    }

    /**
     * Evaluate on this expression has no meaning, but we return a String copy of this expression for debug purpose
     */
    @Override
    public Object evaluate(IEvaluationContext context) {
        return toString();
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return null;
    }

    public List<IExpression> getExpressionList() {
        return expressionList;
    }

    @Override
    public String toString() {
        return "(" + this.getExpressionList().stream().map(Object::toString).collect(Collectors.joining(",")) + ")";
    }
}
