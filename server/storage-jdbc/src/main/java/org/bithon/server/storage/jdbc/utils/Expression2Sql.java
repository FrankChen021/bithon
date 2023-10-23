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
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.function.IDataType;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.server.storage.datasource.DataSourceSchema;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/8/17 22:08
 */
public class Expression2Sql extends ExpressionSerializer {

    public static String from(DataSourceSchema schema, ISqlDialect sqlDialect, IExpression expression) {
        return from(schema.getDataStoreSpec().getStore(), sqlDialect, expression, true);
    }

    public static String from(String schema, ISqlDialect sqlDialect, IExpression expression) {
        return from(schema, sqlDialect, expression, true);
    }

    public static String from(String schema, ISqlDialect sqlDialect, IExpression expression, boolean quoteIdentifier) {
        if (expression == null) {
            return null;
        }
        return new Expression2Sql(schema, quoteIdentifier).serialize(sqlDialect.transform(expression));
    }

    public Expression2Sql(String qualifier, boolean quoteIdentifier) {
        super(qualifier, quoteIdentifier);
    }

    @Override
    public boolean visit(LiteralExpression expression) {
        Object value = expression.getValue();
        if (IDataType.STRING.equals(expression.getDataType())) {
            sb.append('\'');
            // Escape the single quote to ensure the user input is safe
            sb.append(((String) value).replace("'", "\\'"));
            sb.append('\'');
        } else if (expression.isNumber()) {
            sb.append(value);
        } else if (expression.getDataType().equals(IDataType.BOOLEAN)) {
            // Some old versions of CK do not support true/false literal, we use integer instead
            sb.append(expression.asBoolean() ? 1 : 0);
        } else {
            throw new RuntimeException("Not supported type " + expression.getDataType());
        }
        return false;
    }
}

