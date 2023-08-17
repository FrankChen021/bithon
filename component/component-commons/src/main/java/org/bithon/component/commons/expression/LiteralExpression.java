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
public class LiteralExpression implements IExpression {
    private final Object value;

    public LiteralExpression(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String getType() {
        return "literal";
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        return value;
    }

    @Override
    public void serializeToText(StringBuilder sb) {
        if (value instanceof String) {
            sb.append('\'');
            sb.append(value);
            sb.append('\'');
        } else {
            sb.append(value);
        }
    }
}
