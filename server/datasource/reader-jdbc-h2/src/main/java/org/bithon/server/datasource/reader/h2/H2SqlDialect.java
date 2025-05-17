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
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.function.AbstractFunction;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.ast.OrderByElement;
import org.bithon.server.datasource.reader.jdbc.statement.ast.WindowFunctionExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Frank Chen
 * @date 17/4/23 11:20 pm
 */
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
    public IExpression toISO8601TimestampExpression(TimeSpan timeSpan) {
        return LiteralExpression.StringLiteral.ofString(timeSpan.toISO8601());
    }

    @Override
    public String stringAggregator(String field) {
        return StringUtils.format("group_concat(\"%s\")", field);
    }

    static class ToUnixTimestampFunction extends AbstractFunction {
        public ToUnixTimestampFunction() {
            super("UNIX_TIMESTAMP", IDataType.LONG, IDataType.LONG);
        }

        @Override
        public Object evaluate(List<Object> args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDeterministic() {
            return true;
        }
    }

    @Override
    public WindowFunctionExpression firstWindowFunction(String field, long window) {
        return WindowFunctionExpression.builder()
                                       .name("FIRST_VALUE")
                                       .args(new ArrayList<>(List.of(new IdentifierExpression(field))))
                                       .partitionBy(new ArithmeticExpression.MUL(
                                           new ArithmeticExpression.DIV(new FunctionExpression(new ToUnixTimestampFunction(), List.of(new IdentifierExpression("timestamp"))), LiteralExpression.of(window)),
                                           LiteralExpression.of(window)
                                       ))
                                       .orderBy(new OrderByElement(IdentifierExpression.of("timestamp")))
                                       .build();
    }

    @Override
    public boolean useWindowFunctionAsAggregator(String aggregator) {
        return AggregateFunction.First.INSTANCE.getName().equals(aggregator)
               || AggregateFunction.Last.INSTANCE.getName().equals(aggregator);
    }

    @Override
    public IExpression transform(ISchema schema, IExpression expression, QuerySettings settings) {
        return expression == null ? null : expression.accept(new H2ExpressionTransformer());
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
