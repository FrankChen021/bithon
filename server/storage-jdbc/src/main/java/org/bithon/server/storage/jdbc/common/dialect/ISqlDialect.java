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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.server.commons.time.TimeSpan;

/**
 * Since we're writing some complex SQLs, we have to deal with different SQL syntax on different DBMS
 * @author frank.chen021@outlook.com
 * @date 2021-10-26
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ISqlDialect {

    String quoteIdentifier(String identifier);

    /**
     * different DBMS has different functions to support time_floor semantics
     *
     * @param intervalSeconds in seconds
     */
    String timeFloorExpression(IExpression timestampExpression, long intervalSeconds);

    boolean isAliasAllowedInWhereClause();

    /**
     * Some DBMSs, like MySQL, require table alias if there are nested queries
     */
    boolean needTableAlias();

    default String formatTimestamp(TimeSpan timeSpan) {
        return "'" + timeSpan.toISO8601() + "'";
    }

    IExpression toTimestampExpression(TimeSpan timeSpan);

    String stringAggregator(String field);

    String firstAggregator(String field, long window);

    /**
     * @param window in seconds
     */
    String lastAggregator(String field, long window);

    default boolean useWindowFunctionAsAggregator(String aggregator) {
        return false;
    }

    /**
     * Transform expressions for the target dialect
     */
    default IExpression transform(IExpression expression) {
        return expression;
    }

    String formatDateTime(LiteralExpression.TimestampLiteral expression);

    /**
     * The escape character that is used to escape the single quote character in SQL
     */
    char getEscapeCharacter4SingleQuote();
}
