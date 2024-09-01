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

package org.bithon.server.storage.jdbc.h2;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.MapAccessExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.expression.optimzer.ExpressionOptimizer;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.jdbc.common.dialect.MapAccessExpressionTransformer;

import java.util.Arrays;

/**
 * @author Frank Chen
 * @date 17/4/23 11:20 pm
 */
@JsonTypeName("h2")
public class H2SqlDialect implements ISqlDialect {

    @Override
    public String quoteIdentifier(String identifier) {
        return "\"" + identifier + "\"";
    }

    @Override
    public String timeFloorExpression(IExpression timestampExpression, long intervalSeconds) {
        return StringUtils.format("UNIX_TIMESTAMP(%s)/ %d * %d", timestampExpression.serializeToText(), intervalSeconds, intervalSeconds);
    }

    @Override
    public boolean isAliasAllowedInWhereClause() {
        return false;
    }

    @Override
    public boolean needTableAlias() {
        return false;
    }

    @Override
    public String stringAggregator(String field) {
        return StringUtils.format("group_concat(\"%s\")", field);
    }

    @Override
    public String firstAggregator(String field, long window) {
        return StringUtils.format(
            "FIRST_VALUE(\"%s\") OVER (partition by %s ORDER BY \"timestamp\")",
            field,
            this.timeFloorExpression(new IdentifierExpression("timestamp"), window));
    }

    @Override
    public String lastAggregator(String field, long window) {
        // NOTE: use FIRST_VALUE instead of LAST_VALUE because the latter one returns the wrong result
        return StringUtils.format(
            "FIRST_VALUE(\"%s\") OVER (partition by %s ORDER BY \"timestamp\" DESC)",
            field,
            this.timeFloorExpression(new IdentifierExpression("timestamp"), window));
    }

    @Override
    public boolean useWindowFunctionAsAggregator(String aggregator) {
        return AggregateFunction.First.NAME.equals(aggregator)
               || AggregateFunction.Last.NAME.equals(aggregator);
    }

    @Override
    public IExpression transform(IExpression expression) {
        return expression.accept(new ExpressionOptimizer.AbstractOptimizer() {
            /**
             * H2 does not support Map, the JSON formatted string is stored in the column.
             * So we turn the MapAccessExpression into a LIKE expression
             */
            @Override
            public IExpression visit(ConditionalExpression expression) {
                if (expression.getLhs() instanceof MapAccessExpression) {
                    return MapAccessExpressionTransformer.transform(expression);
                }

                if (expression instanceof ConditionalExpression.Contains) {
                    return new ConditionalExpression.Like(expression.getLhs(),
                                                          LiteralExpression.ofString("%" + ((LiteralExpression<?>) expression.getRhs()).asString() + "%"));
                }
                if (expression instanceof ConditionalExpression.StartsWith) {
                    return new ConditionalExpression.Like(expression.getLhs(),
                                                          LiteralExpression.ofString(((LiteralExpression<?>) expression.getRhs()).asString() + "%"));
                }
                if (expression instanceof ConditionalExpression.EndsWith) {
                    return new ConditionalExpression.Like(expression.getLhs(),
                                                          LiteralExpression.ofString("%" + ((LiteralExpression<?>) expression.getRhs()).asString()));
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
                    return new ConditionalExpression.Like(expression.getArgs().get(0),
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
                    return new ConditionalExpression.Like(expression.getArgs().get(0),
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
                    return new ConditionalExpression.Like(expression.getArgs().get(0),
                                                          patternExpression);
                }

                return expression;
            }
        });
    }

    @Override
    public String formatDateTime(LiteralExpression.TimestampLiteral expression) {
        return "'" + DateTime.toISO8601(expression.getValue()) + "'";
    }

    /**
     * The H2 uses single quote to escape
     */
    @Override
    public char getEscapeCharacter4SingleQuote() {
        return '\'';
    }
}
