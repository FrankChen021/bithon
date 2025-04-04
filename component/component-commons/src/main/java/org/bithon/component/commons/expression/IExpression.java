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
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/7 20:16
 */
public interface IExpression {

    IDataType getDataType();

    String getType();

    Object evaluate(IEvaluationContext context);

    void accept(IExpressionInDepthVisitor visitor);

    <T> T accept(IExpressionVisitor<T> visitor);

    default String serializeToText() {
        return serializeToText(IdentifierQuotaStrategy.DOUBLE_QUOTE);
    }

    default String serializeToText(IdentifierQuotaStrategy strategy) {
        ExpressionSerializer serializer = new ExpressionSerializer(strategy);
        serializeToText(serializer);
        return serializer.getSerializedText();
    }

    void serializeToText(ExpressionSerializer serializer);
}
