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
 * @author Frank Chen
 * @date 30/6/23 6:13 pm
 */
public class FieldExpression implements IExpression {

    private final String name;

    private final boolean isQualified;

    public FieldExpression(String name) {
        this.name = name;
        this.isQualified = name.indexOf('.') > 0;
    }

    public String getName() {
        return name;
    }

    public boolean isQualified() {
        return isQualified;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        return context.get(name);
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
