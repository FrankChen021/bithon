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

package org.bithon.server.datasource.reader.h2;


import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.MapAccessExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.function.builtin.StringFunction;
import org.bithon.component.commons.expression.optimzer.AbstractOptimizer;
import org.bithon.server.datasource.reader.jdbc.dialect.LikeOperator;
import org.bithon.server.datasource.reader.jdbc.dialect.MapAccessExpressionTransformer;

import java.util.Arrays;

/**
 * @author frank.chen021@outlook.com
 * @date 17/5/25 12:14 pm
 */
class H2ExpressionTransformer extends AbstractOptimizer {

    /**
     * H2 does not support Map, the JSON formatted string is stored in the column.
     * So we turn the MapAccessExpression into a LIKE expression
     */
    @Override
    public IExpression visit(ConditionalExpression expression) {
        if (expression.getLhs() instanceof MapAccessExpression) {
            return MapAccessExpressionTransformer.transform(expression);
        }

        if (expression instanceof ConditionalExpression.StartsWith) {
            return new LikeOperator(expression.getLhs(),
                                    LiteralExpression.ofString(((LiteralExpression<?>) expression.getRhs()).asString() + "%"));
        }
        if (expression instanceof ConditionalExpression.EndsWith) {
            return new LikeOperator(expression.getLhs(),
                                    LiteralExpression.ofString("%" + ((LiteralExpression<?>) expression.getRhs()).asString()));
        }
        if (expression instanceof ConditionalExpression.HasToken) {
            return new FunctionExpression(StringFunction.HasToken.INSTANCE, expression.getLhs(), expression.getRhs());
        }

        if (expression instanceof ConditionalExpression.RegularExpressionMatchExpression) {
            return toRegexpLikeExpression(expression);
        }

        if (expression instanceof ConditionalExpression.RegularExpressionNotMatchExpression) {
            return new LogicalExpression.NOT(toRegexpLikeExpression(expression));
        }

        return super.visit(expression);
    }

    @Override
    public IExpression visit(FunctionExpression expression) {
        if ("startsWith".equals(expression.getName())) {
            // H2 does not provide startsWith function, turns it into LIKE expression as: LIKE 'prefix%'
            IExpression patternExpression = expression.getArgs().get(1);
            if (patternExpression instanceof LiteralExpression) {
                patternExpression = LiteralExpression.ofString(((LiteralExpression<?>) patternExpression).getValue() + "%");
            } else {
                patternExpression = new FunctionExpression(Functions.getInstance().getFunction("concat"),
                                                           Arrays.asList(patternExpression, LiteralExpression.ofString("%")));
            }
            return new LikeOperator(expression.getArgs().get(0),
                                    patternExpression);
        } else if ("endsWith".equals(expression.getName())) {
            // H2 does not provide endsWith function, turns it into LIKE expression as: LIKE '%prefix'
            IExpression patternExpression = expression.getArgs().get(1);
            if (patternExpression instanceof LiteralExpression) {
                patternExpression = LiteralExpression.ofString("%" + ((LiteralExpression<?>) patternExpression).getValue());
            } else {
                patternExpression = new FunctionExpression(Functions.getInstance().getFunction("concat"),
                                                           Arrays.asList(LiteralExpression.ofString("%"), patternExpression));
            }
            return new LikeOperator(expression.getArgs().get(0),
                                    patternExpression);

        } else if ("hasToken".equals(expression.getName())) {
            // H2 does not provide hasToken function, turns it into LIKE expression as: LIKE '%prefix'
            IExpression patternExpression = expression.getArgs().get(1);
            if (patternExpression instanceof LiteralExpression) {
                patternExpression = LiteralExpression.ofString("%" + ((LiteralExpression<?>) patternExpression).getValue() + "%");
            } else {
                patternExpression = new FunctionExpression(Functions.getInstance().getFunction("concat"),
                                                           Arrays.asList(LiteralExpression.ofString("%"), patternExpression));
            }
            return new LikeOperator(expression.getArgs().get(0),
                                    patternExpression);
        }

        return super.visit(expression);
    }

    @Override
    public IExpression visit(ArithmeticExpression expression) {
        return SafeDivisionTransformer.transform(expression);
    }

    private IExpression toRegexpLikeExpression(BinaryExpression expr) {
        return new FunctionExpression("regexp_like",
                                      expr.getLhs(),
                                      expr.getRhs(),
                                      // https://www.h2database.com/html/functions.html?utm_source=chatgpt.com#regexp_like
                                      // Supports multiline and newline mode
                                      LiteralExpression.ofString("nm"));
    }
}
