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

package org.bithon.server.datasource.reader.clickhouse;

import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.function.builtin.TimeFunction;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.bithon.server.datasource.reader.clickhouse.expression.ClickHouseExpressionOptimizer;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.serializer.Expression2Sql;

/**
 * @author frank.chen021@outlook.com
 * @date 1/11/21 5:21 pm
 */
public class ClickHouseSqlDialect implements ISqlDialect {

    @Override
    public Expression2Sql createSqlSerializer(String qualifier) {
        return new Expression2Sql(qualifier, this) {
            private IExpression toRegularExpression(BinaryExpression expr) {
                return new FunctionExpression("match",
                                              expr.getLhs(),
                                              expr.getRhs());
            }

            @Override
            public void serialize(BinaryExpression binaryExpression) {
                if (binaryExpression instanceof ConditionalExpression.RegularExpressionMatchExpression) {
                    // TODO: optimize to startsWith/endsWith/like/eq which has better performance
                    this.serialize(toRegularExpression(binaryExpression));
                } else if (binaryExpression instanceof ConditionalExpression.RegularExpressionNotMatchExpression) {
                    this.serialize(new LogicalExpression.NOT(toRegularExpression(binaryExpression)));
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
        if (intervalSeconds == 60L) {
            if (timestampExpression instanceof FunctionExpression) {
                if ("toStartOfMinute".equals(((FunctionExpression) timestampExpression).getName())) {
                    // If the user already specifies the toStartOfMinute function call,
                    // there's no need to apply the function once more
                    return StringUtils.format("toUnixTimestamp(%s)", timestampExpression.serializeToText(this::quoteIdentifier));
                }
            }
            return StringUtils.format("toUnixTimestamp(toStartOfMinute(%s))", timestampExpression.serializeToText(this::quoteIdentifier));
        }
        if (intervalSeconds == 60 * 5) {
            return StringUtils.format("toUnixTimestamp(toStartOfFiveMinute(%s))", timestampExpression.serializeToText(this::quoteIdentifier));
        }
        if (intervalSeconds == 60 * 15) {
            return StringUtils.format("toUnixTimestamp(toStartOfFifteenMinutes(%s))", timestampExpression.serializeToText(this::quoteIdentifier));
        }
        if (intervalSeconds == 3600) {
            return StringUtils.format("toUnixTimestamp(toStartOfHour(%s))", timestampExpression.serializeToText(this::quoteIdentifier));
        }

        if (intervalSeconds > 60 && intervalSeconds % 60 == 0) {
            return StringUtils.format("toUnixTimestamp(toStartOfInterval(%s, INTERVAL %d MINUTE))",
                                      timestampExpression.serializeToText(this::quoteIdentifier),
                                      intervalSeconds / 60);
        } else {
            return StringUtils.format("toUnixTimestamp(toStartOfInterval(%s, INTERVAL %d SECOND))", timestampExpression.serializeToText(this::quoteIdentifier), intervalSeconds);
        }
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
        return new FunctionExpression(TimeFunction.FromUnixTimestamp.INSTANCE, LiteralExpression.LongLiteral.ofLong(timeSpan.getMilliseconds() / 1000));
    }

    @Override
    public String stringAggregator(String field) {
        return StringUtils.format("arrayStringConcat(arrayCompact(arrayFilter(x -> x <> '', groupArray(%s))), ',')",
                                  field);
    }

    @Override
    public String formatDateTime(LiteralExpression.TimestampLiteral expression) {
        return StringUtils.format("fromUnixTimestamp64Milli(%d)", expression.getValue());
    }

    @Override
    public char getEscapeCharacter4SingleQuote() {
        return '\\';
    }

    @Override
    public IExpression transform(ISchema schema, IExpression expression, QuerySettings querySettings) {
        return expression == null ? null : expression.accept(new ClickHouseExpressionOptimizer(schema, querySettings));
    }
}
