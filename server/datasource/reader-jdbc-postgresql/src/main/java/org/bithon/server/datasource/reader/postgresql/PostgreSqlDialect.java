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

package org.bithon.server.datasource.reader.postgresql;

import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.MapAccessExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.expression.function.builtin.StringFunction;
import org.bithon.component.commons.expression.optimzer.AbstractOptimizer;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.dialect.LikeOperator;
import org.bithon.server.datasource.reader.jdbc.dialect.MapAccessExpressionTransformer;
import org.bithon.server.datasource.reader.jdbc.statement.ast.OrderByElement;
import org.bithon.server.datasource.reader.jdbc.statement.ast.WindowFunctionExpression;
import org.bithon.server.datasource.reader.jdbc.statement.serializer.Expression2Sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Frank Chen
 * @date 17/4/23 11:20 pm
 */
public class PostgreSqlDialect implements ISqlDialect {

    @Override
    public Expression2Sql createSqlSerializer(String qualifier) {
        return new Expression2Sql(qualifier, this) {
            @Override
            public void serialize(BinaryExpression binaryExpression) {
                if (binaryExpression instanceof ConditionalExpression.RegularExpressionMatchExpression) {
                    this.serialize(binaryExpression.getLhs());
                    sb.append(" ~ ");
                    this.serialize(binaryExpression.getRhs());
                } else if (binaryExpression instanceof ConditionalExpression.RegularExpressionNotMatchExpression) {
                    this.append("NOT (");
                    this.serialize(binaryExpression.getLhs());
                    sb.append(" ~ ");
                    this.serialize(binaryExpression.getRhs());
                    this.append(")");
                } else {
                    super.serialize(binaryExpression);
                }
            }
        };
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return "\"" + identifier + "\"";
    }

    @Override
    public String timeFloorExpression(IExpression timestampExpression, long intervalSeconds) {
        //TODO: Fix the hardcode UTC-8
        return StringUtils.format(" FLOOR(EXTRACT(EPOCH FROM %s AT TIME ZONE 'UTC-8') / %d) * %d", timestampExpression.serializeToText(), intervalSeconds, intervalSeconds);
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
    public IExpression toISO8601TimestampExpression(TimeSpan timeSpan) {
        return LiteralExpression.StringLiteral.ofString(timeSpan.toISO8601());
    }

    @Override
    public String stringAggregator(String field) {
        return StringUtils.format("group_concat(\"%s\")", field);
    }

    /**
     * FIRST_VALUE(`%s`) OVER (partition by %s ORDER BY `timestamp` ASC)
     */
    @Override
    public WindowFunctionExpression firstWindowFunction(String field, long window) {
        return WindowFunctionExpression.builder()
                                       .name("FIRST_VALUE")
                                       .args(new ArrayList<>(List.of(new IdentifierExpression(field))))
                                       .partitionBy(new ArithmeticExpression.MUL(
                                           new ArithmeticExpression.DIV(new IdentifierExpression("timestamp"), LiteralExpression.of(window)),
                                           LiteralExpression.of(window)
                                       ))
                                       .orderBy(new OrderByElement(new IdentifierExpression("timestamp")))
                                       .build();
    }

    @Override
    public boolean useWindowFunctionAsAggregator(String aggregator) {
        return AggregateFunction.First.INSTANCE.getName().equals(aggregator)
               || AggregateFunction.Last.INSTANCE.getName().equals(aggregator);
    }

    @Override
    public IExpression transform(ISchema schema, IExpression expression) {
        return expression == null ? null : expression.accept(new AbstractOptimizer() {
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
                    return this.visit(new FunctionExpression(StringFunction.HasToken.INSTANCE, expression.getLhs(), expression.getRhs()));
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
                    return new LikeOperator(expression.getArgs().get(0), patternExpression);
                } else if ("endsWith".equals(expression.getName())) {
                    // H2 does not provide endsWith function, turns it into LIKE expression as: LIKE '%prefix'
                    IExpression patternExpression = expression.getArgs().get(1);
                    if (patternExpression instanceof LiteralExpression) {
                        patternExpression = LiteralExpression.ofString("%" + ((LiteralExpression<?>) patternExpression).getValue());
                    } else {
                        patternExpression = new FunctionExpression(Functions.getInstance().getFunction("concat"),
                                                                   Arrays.asList(LiteralExpression.ofString("%"), patternExpression));
                    }
                    return new LikeOperator(expression.getArgs().get(0), patternExpression);

                } else if ("hasToken".equals(expression.getName())) {
                    // H2 does not provide hasToken function, turns it into LIKE expression as: LIKE '%prefix'
                    IExpression patternExpression = expression.getArgs().get(1);
                    if (patternExpression instanceof LiteralExpression) {
                        patternExpression = LiteralExpression.ofString("%" + ((LiteralExpression<?>) patternExpression).getValue() + "%");
                    } else {
                        patternExpression = new FunctionExpression(Functions.getInstance().getFunction("concat"),
                                                                   Arrays.asList(LiteralExpression.ofString("%"), patternExpression));
                    }
                    return new LikeOperator(expression.getArgs().get(0), patternExpression);
                }

                return super.visit(expression);
            }

            @Override
            public IExpression visit(ArithmeticExpression expression) {
                return SafeDivisionTransformer.transform(expression);
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
