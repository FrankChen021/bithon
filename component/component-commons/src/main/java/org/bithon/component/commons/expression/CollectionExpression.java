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
public class CollectionExpression implements IExpression {
    private final List<IExpression> elements;
    private final Set<Object> values;

    public CollectionExpression(IExpression... elements) {
        this(Arrays.asList(elements));
    }

    public CollectionExpression(List<IExpression> elements) {
        this.elements = elements;

        // Only literal is supported now
        this.values = this.elements.stream().map((element) -> ((LiteralExpression) element).getValue()).collect(Collectors.toSet());
    }

    public List<IExpression> getElements() {
        return elements;
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
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void serializeToText(StringBuilder sb) {
        sb.append('(');
        sb.append(values.stream().map((val) -> val instanceof String ? "'" + val + "'" : val.toString()).collect(Collectors.joining(", ")));
        sb.append(')');
    }
}
