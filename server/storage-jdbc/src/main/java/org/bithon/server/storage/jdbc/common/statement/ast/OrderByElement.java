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

package org.bithon.server.storage.jdbc.common.statement.ast;


import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;

/**
 * @author frank.chen021@outlook.com
 * @date 22/4/25 11:01 pm
 */
public class OrderByElement implements IExpression {
    private final IExpression name;
    private final OrderByDirection direction;

    public OrderByElement(IExpression name) {
        this.name = name;
        this.direction = OrderByDirection.ASC;
    }

    public OrderByElement(IExpression name, OrderByDirection direction) {
        this.name = name;
        this.direction = direction;
    }

    @Override
    public IDataType getDataType() {
        return null;
    }

    @Override
    public String getType() {
        return "";
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        return null;
    }

    @Override
    public void accept(IExpressionInDepthVisitor visitor) {

    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return null;
    }

    @Override
    public void serializeToText(ExpressionSerializer serializer) {
        name.serializeToText(serializer);
        serializer.append(' ');
        serializer.append(direction.name());
    }
}
