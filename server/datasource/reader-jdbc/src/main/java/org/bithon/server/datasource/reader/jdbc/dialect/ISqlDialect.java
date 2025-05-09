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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.reader.jdbc.statement.ast.WindowFunctionExpression;
import org.bithon.server.datasource.reader.jdbc.statement.serializer.Expression2Sql;

import java.sql.Timestamp;

/**
 * Since we're writing some complex SQLs, we have to deal with different SQL syntax on different DBMS
 *
 * @author frank.chen021@outlook.com
 * @date 2021-10-26
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ISqlDialect {

    Expression2Sql createSqlSerializer(String qualifier);

    String quoteIdentifier(String identifier);

    /**
     * different DBMS has different functions to support time_floor semantics
     *
     * @param intervalSeconds in seconds
     * @return an expression that returns the timestamp in seconds
     */
    String timeFloorExpression(IExpression timestampExpression, long intervalSeconds);

    boolean isAliasAllowedInWhereClause();

    /**
     * Some DBMSs, like MySQL, require table alias if there are nested queries
     */
    boolean needTableAlias();

    IExpression toISO8601TimestampExpression(TimeSpan timeSpan);

    default IExpression toISO8601TimestampExpression(Timestamp timestamp) {
        return toISO8601TimestampExpression(TimeSpan.of(timestamp.getTime()));
    }

    String stringAggregator(String field);

    default WindowFunctionExpression firstWindowFunction(String field, long window) {
        throw new UnsupportedOperationException();
    }

    default boolean useWindowFunctionAsAggregator(String aggregator) {
        return false;
    }

    /**
     * Transform expressions for the target dialect
     */
    default IExpression transform(IExpression expression) {
        return transform(null, expression);
    }

    default IExpression transform(ISchema schema, IExpression expression) {
        return expression;
    }

    String formatDateTime(LiteralExpression.TimestampLiteral expression);

    /**
     * The escape character that is used to escape the single quote character in SQL
     */
    char getEscapeCharacter4SingleQuote();
}
