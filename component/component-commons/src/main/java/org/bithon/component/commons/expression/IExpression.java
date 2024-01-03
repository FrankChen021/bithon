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

import java.util.function.Function;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/7 20:16
 */
public interface IExpression {

    String getType();

    Object evaluate(IEvaluationContext context);

    void accept(IExpressionVisitor visitor);

    <T> T accept(IExpressionVisitor2<T> visitor);

    default String serializeToText() {
        return serializeToText((s) -> "\"" + s + "\"");
    }

    default String serializeToText(Function<String, String> quoteIdentifier) {
        ExpressionSerializer serializer = new ExpressionSerializer(quoteIdentifier);
        return serializer.serialize(this);
    }
}
