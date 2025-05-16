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

package org.bithon.server.datasource.reader.jdbc.statement.ast;


import lombok.Getter;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;

/**
 * example:
 * sum("t1") OVER (PARTITION BY "appName" ORDER BY "_timestamp" ASC RANGE BETWEEN 60 PRECEDING AND 0 FOLLOWING)
 *
 * @author frank.chen021@outlook.com
 * @date 21/4/25 9:29 pm
 */
public class WindowFunctionFrame implements IExpression {
    @Getter
    private final IExpression start;

    @Getter
    private final IExpression end;

    public WindowFunctionFrame(IExpression start, IExpression end) {
        this.start = start;
        this.end = end;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(IExpressionInDepthVisitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void serializeToText(ExpressionSerializer serializer) {
        serializer.append("RANGE BETWEEN ");
        serializer.serialize(start);
        serializer.append(" PRECEDING AND ");
        serializer.serialize(end);
        serializer.append(" FOLLOWING");
    }
}
