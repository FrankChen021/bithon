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

package org.bithon.server.datasource.reader.jdbc.dialect;

import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.utils.SqlLikeExpression;
import org.bithon.server.datasource.ISchema;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/8/17 22:08
 */
public class Expression2Sql extends ExpressionSerializer {

    public static String from(ISchema schema,
                              ISqlDialect sqlDialect,
                              IExpression expression) {
        return from(schema.getDataStoreSpec().getStore(),
                    sqlDialect,
                    expression);
    }

    public static String from(String qualifier,
                              ISqlDialect sqlDialect,
                              IExpression expression) {
        if (expression == null) {
            return null;
        }

        // Apply DB-related transformation on general AST
        IExpression transformed = sqlDialect.transform(null, expression);

        return new Expression2Sql(qualifier, sqlDialect).serialize(transformed);
    }

    protected final ISqlDialect sqlDialect;

    public Expression2Sql(String qualifier, ISqlDialect sqlDialect) {
        super(qualifier, sqlDialect::quoteIdentifier);
        this.sqlDialect = sqlDialect;
    }

    @Override
    public void serialize(LiteralExpression<?> expression) {
        if (expression instanceof LiteralExpression.StringLiteral stringLiteral) {
            sb.append('\'');
            // Escape the single quote to ensure the user input is safe
            sb.append(StringUtils.escape(stringLiteral.getValue(), sqlDialect.getEscapeCharacter4SingleQuote(), '\''));
            sb.append('\'');
        } else if (expression instanceof LiteralExpression.LongLiteral longLiteral) {
            sb.append(longLiteral.getValue());
        } else if (expression instanceof LiteralExpression.DoubleLiteral doubleLiteral) {
            sb.append(doubleLiteral.getValue());
        } else if (expression instanceof LiteralExpression.BooleanLiteral) {
            // Some old versions of CK do not support true/false literal, we use integer instead
            sb.append(expression.asBoolean() ? 1 : 0);
        } else if (expression instanceof LiteralExpression.TimestampLiteral) {
            sb.append(sqlDialect.formatDateTime((LiteralExpression.TimestampLiteral) expression));
        } else if (expression instanceof LiteralExpression.AsteriskLiteral) {
            sb.append('*');
        } else if (expression instanceof LiteralExpression.ReadableDurationLiteral durationLiteral) {
            sb.append(durationLiteral.getValue().getDuration().getSeconds());
        } else if (expression instanceof LiteralExpression.ReadableNumberLiteral numberLiteral) {
            sb.append(numberLiteral.getValue().longValue());
        } else if (expression instanceof LiteralExpression.ReadablePercentageLiteral percentageLiteral) {
            sb.append(percentageLiteral.getValue().getFraction());
        } else {
            throw new RuntimeException("Not supported type " + expression.getDataType());
        }
    }

    @Override
    public void serialize(FunctionExpression expression) {
        if (expression.getFunction() instanceof AggregateFunction.Count
            && expression.getArgs().isEmpty()) {
            // Some DBMSs require parameter on the 'count' function
            sb.append("count(1)");
            return;
        }

        super.serialize(expression);
    }

    /**
     * Transform the 'contains' operator into 'LIKE' operator.
     * The pattern in the 'contains' operator will be escaped if necessary.
     */
    @Override
    public void serialize(BinaryExpression expression) {
        if (expression instanceof ConditionalExpression.Contains) {
            String pattern = ((LiteralExpression<?>) expression.getRhs()).asString();

            super.serialize(new LikeOperator(expression.getLhs(),
                                             LiteralExpression.ofString(SqlLikeExpression.toLikePattern(pattern))));
        } else {
            super.serialize(expression);
        }
    }
}

