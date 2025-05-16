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

package org.bithon.server.web.service.datasource.api.impl;

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.expression.ExpressionASTBuilder;

import javax.annotation.Nullable;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/5/6 23:49
 */
public class QueryFilter {

    public static IExpression build(ISchema schema, @Nullable String filterExpression) {
        if (StringUtils.isEmpty(filterExpression)) {
            return null;
        }

        IExpression expr = ExpressionASTBuilder.builder()
                                               .schema(schema)
                                               .functions(Functions.getInstance())
                                               .build(filterExpression);
        return validateIfConditional(expr);
    }

    /**
     * Validate if the expression is a filter and optimize if possible
     */
    private static IExpression validateIfConditional(IExpression expression) {
        if (expression instanceof FunctionExpression) {
            return switch (expression.getDataType()) {
                case STRING ->
                    throw new InvalidExpressionException("Function expression [%s] returns type of String, is not a valid filter. Consider to add comparators to your expression.",
                                                         expression.serializeToText());
                case LONG, DOUBLE ->
                    // Turn into: functionExpression <> 0
                    new ComparisonExpression.NE(expression, LiteralExpression.ofLong(0));
                case BOOLEAN -> expression;
                default ->
                    throw new InvalidExpressionException("Function expression [%s] returns type of %s, is not a valid filter. Consider to add comparators to your expression.",
                                                         expression.serializeToText(),
                                                         expression.getDataType());
            };
        }

        if (!(expression instanceof LogicalExpression)
            && !(expression instanceof ConditionalExpression)) {
            throw new InvalidExpressionException("Expression [%s] is not a valid filter. Consider to add operators to your expression.",
                                                 expression.serializeToText());
        }

        return expression;
    }
}
