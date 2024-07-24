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

package org.bithon.server.storage.jdbc.mysql;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.MapAccessExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.optimzer.ExpressionOptimizer;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.query.ast.QueryAggregateFunctions;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.jdbc.common.dialect.MapAccessExpressionTransformer;

import java.util.Arrays;

/**
 * @author Frank Chen
 * @date 17/4/23 11:20 pm
 */
@JsonTypeName("mysql")
public class MySQLSqlDialect implements ISqlDialect {

    @Override
    public String quoteIdentifier(String identifier) {
        return "`" + identifier + "`";
    }

    @Override
    public String timeFloorExpression(IExpression timestampExpression, long interval) {
        return StringUtils.format("UNIX_TIMESTAMP(`%s`) div %d * %d", timestampExpression.serializeToText(null), interval, interval);
    }

    @Override
    public boolean groupByUseRawExpression() {
        return true;
    }

    @Override
    public boolean allowSameAggregatorExpression() {
        return true;
    }

    @Override
    public String stringAggregator(String field) {
        return StringUtils.format("group_concat(`%s`)", field);
    }

    @Override
    public String firstAggregator(String field, String name, long window) {
        return StringUtils.format(
            "FIRST_VALUE(`%s`) OVER (partition by %s ORDER BY `timestamp`) AS `%s`",
            field,
            this.timeFloorExpression(new IdentifierExpression("timestamp"), window),
            name);
    }

    @Override
    public String lastAggregator(String field, long window) {
        // NOTE: use FIRST_VALUE instead of LAST_VALUE because the latter one returns the wrong result
        return StringUtils.format(
            "FIRST_VALUE(`%s`) OVER (partition by %s ORDER BY `timestamp` DESC)",
            field,
            this.timeFloorExpression(new IdentifierExpression("timestamp"), window));
    }

    @Override
    public boolean useWindowFunctionAsAggregator(String aggregator) {
        return QueryAggregateFunctions.FirstAggregateExpression.TYPE.equals(aggregator)
               || QueryAggregateFunctions.LastAggregateExpression.TYPE.equals(aggregator);
    }

    @Override
    public IExpression transform(IExpression expression) {
        return expression.accept(new ExpressionOptimizer.AbstractOptimizer() {
            /**
             * MYSQL does not support Map, the JSON formatted string is stored in the column.
             * So we turn the MapAccessExpression into a LIKE expression
             */
            @Override
            public IExpression visit(ConditionalExpression expression) {
                if (expression.getLeft() instanceof MapAccessExpression) {
                    return MapAccessExpressionTransformer.transform(expression);
                }

                if (expression instanceof ConditionalExpression.Contains) {
                    return new ConditionalExpression.Like(expression.getLeft(),
                                                          LiteralExpression.create("%" + ((LiteralExpression) expression.getRight()).asString() + "%"));
                }
                if (expression instanceof ConditionalExpression.StartsWith) {
                    return new ConditionalExpression.Like(expression.getLeft(),
                                                          LiteralExpression.create(((LiteralExpression) expression.getRight()).asString() + "%"));
                }
                if (expression instanceof ConditionalExpression.EndsWith) {
                    return new ConditionalExpression.Like(expression.getLeft(),
                                                          LiteralExpression.create("%" + ((LiteralExpression) expression.getRight()).asString()));
                }

                return super.visit(expression);
            }

            @Override
            public IExpression visit(FunctionExpression expression) {
                if ("startsWith".equals(expression.getName())) {
                    // MySQL does not provide startsWith function, turns it into LIKE expression as: LIKE 'prefix%'
                    IExpression patternExpression = expression.getParameters().get(1);
                    if (patternExpression instanceof LiteralExpression) {
                        patternExpression = LiteralExpression.create(((LiteralExpression) patternExpression).getValue() + "%");
                    } else {
                        patternExpression = new FunctionExpression(Functions.getInstance().getFunction("concat"),
                                                                   Arrays.asList(patternExpression, LiteralExpression.create("%")));
                    }
                    return new ConditionalExpression.Like(expression.getParameters().get(0),
                                                          patternExpression);
                } else if ("endsWith".equals(expression.getName())) {
                    // MySQL does not provide endsWith function, turns it into LIKE expression as: LIKE '%prefix'
                    IExpression patternExpression = expression.getParameters().get(1);
                    if (patternExpression instanceof LiteralExpression) {
                        patternExpression = LiteralExpression.create("%" + ((LiteralExpression) patternExpression).getValue());
                    } else {
                        patternExpression = new FunctionExpression(Functions.getInstance().getFunction("concat"),
                                                                   Arrays.asList(LiteralExpression.create("%"), patternExpression));
                    }
                    return new ConditionalExpression.Like(expression.getParameters().get(0),
                                                          patternExpression);
                }

                return expression;
            }
        });
    }

    @Override
    public String formatDateTime(LiteralExpression.TimestampLiteral expression) {
        return "'" + DateTime.toISO8601((long) expression.getValue()) + "'";
    }

    @Override
    public char getEscapeCharacter4SingleQuote() {
        return '\\';
    }
}
