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

package org.bithon.server.datasource.reader.postgresql;


import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.component.commons.utils.StringUtils;

/**
 * the EPOCH FROM xxx AT TIME ZONE 'yyy' expression in PG
 *
 * @author frank.chen021@outlook.com
 * @date 1/11/25 1:38 pm
 */
public class EpochFromExpression implements IExpression {
    private final IExpression column;
    private final String timezone;

    public EpochFromExpression(IdentifierExpression column) {
        this(column, null);
    }

    public EpochFromExpression(IExpression column, String timezone) {
        this.column = column;
        this.timezone = timezone;
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
        serializer.append("EPOCH FROM ");
        this.column.serializeToText(serializer);
        if (timezone != null) {
            serializer.append(StringUtils.format(" AT TIME ZONE '%s'", this.timezone));
        }
    }
}
