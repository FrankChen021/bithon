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

package org.bithon.server.storage.jdbc.utils;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.server.storage.datasource.DataSourceSchema;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/8/17 22:08
 */
public class SqlFilterStatement {
    public static String from(DataSourceSchema schema, IExpression expression) {
        if (expression == null) {
            return null;
        }

        return from(schema, expression, new ExpressionSerializer() {
            @Override
            public boolean visit(IdentifierExpression expression) {
                sb.append('"');
                sb.append(expression.getIdentifier());
                sb.append('"');
                return false;
            }
        });
    }

    public static String from(DataSourceSchema schema, IExpression expression, ExpressionSerializer serializer) {
        if (expression == null) {
            return null;
        }
        return serializer.serialize(expression);
    }
}
