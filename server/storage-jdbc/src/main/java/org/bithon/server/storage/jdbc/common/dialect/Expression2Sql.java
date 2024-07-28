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

package org.bithon.server.storage.jdbc.common.dialect;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.ISchema;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/8/17 22:08
 */
public class Expression2Sql extends ExpressionSerializer {

    public static String from(ISchema dataSource, ISqlDialect sqlDialect, IExpression expression) {
        return from(dataSource.getDataStoreSpec().getStore(), sqlDialect, expression);
    }

    public static String from(String qualifier, ISqlDialect sqlDialect, IExpression expression) {
        if (expression == null) {
            return null;
        }

        // Apply DB-related transformation on general AST
        IExpression transformed = sqlDialect.transform(expression);

        return new Expression2Sql(qualifier, sqlDialect).serialize(transformed);
    }

    protected final ISqlDialect sqlDialect;

    public Expression2Sql(String qualifier, ISqlDialect sqlDialect) {
        super(qualifier, sqlDialect::quoteIdentifier);
        this.sqlDialect = sqlDialect;
    }

    @Override
    public boolean visit(LiteralExpression expression) {
        Object value = expression.getValue();
        if (expression instanceof LiteralExpression.StringLiteral) {
            sb.append('\'');
            // Escape the single quote to ensure the user input is safe
            sb.append(StringUtils.escapeSingleQuoteIfNecessary((String) value, sqlDialect.getEscapeCharacter4SingleQuote()));
            sb.append('\'');
        } else if (expression instanceof LiteralExpression.LongLiteral || expression instanceof LiteralExpression.DoubleLiteral) {
            sb.append(value);
        } else if (expression instanceof LiteralExpression.BooleanLiteral) {
            // Some old versions of CK do not support true/false literal, we use integer instead
            sb.append(expression.asBoolean() ? 1 : 0);
        } else if (expression instanceof LiteralExpression.TimestampLiteral) {
            sb.append(sqlDialect.formatDateTime((LiteralExpression.TimestampLiteral) expression));
        } else {
            throw new RuntimeException("Not supported type " + expression.getDataType());
        }
        return false;
    }
}

