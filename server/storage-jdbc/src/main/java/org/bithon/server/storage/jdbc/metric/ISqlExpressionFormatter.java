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

package org.bithon.server.storage.jdbc.metric;

import org.bithon.server.common.utils.datetime.TimeSpan;

/**
 * Since we're writing some complex SQLs, we have to deal with different SQL syntax on different DBMS
 * @author Frank Chen
 * @date 2021-10-26
 */
public interface ISqlExpressionFormatter {
    /**
     * different DBMS has different functions to support time_floor semantics
     */
    default String timeFloor(String field, long interval) {
        return String.format("UNIX_TIMESTAMP(\"%s\")/ %d * %d", field, interval, interval);
    }

    /**
     * some DBMS requires the group-by expression to be the same as the expression in field list
     * even if field is a complex expression
     *
     * For example, the following group-by expression can't be the alias of the expression in field list.
     * In this case, this function should return 'true'
     * <p>
     * SELECT UNIX_TIMESTAMP("timestamp")/ 10 * 10 "timestamp",
     *        sum("requestBytes")/10 "requestByteRate",
     *        sum("responseBytes")/10 "responseByteRate",
     *        sum("requestBytes") AS "requestBytes",sum("responseBytes") AS "responseBytes"
     * FROM "bithon_redis_metrics" OUTER
     * WHERE "appName"='bithon-server-dev'
     *   AND "timestamp" >= '2021-10-28T16:26:42+08:00' AND "timestamp" <= '2021-10-28T16:31:42+08:00'
     * GROUP BY UNIX_TIMESTAMP("timestamp")/ 10 * 10
     * </p>
     */
    boolean groupByUseRawExpression();

    default String formatTimestamp(TimeSpan timeSpan) {
        return "'" + timeSpan.toISO8601() + "'";
    }

    /**
     * some DBMS returns data out of timestamp order
     * this interface returns a ORDER-BY SQL clause for such DBMS
     */
    default String orderByTimestamp(String timestampField) {
        return "";
    }

    default String getReadTableName(String name) {
        return "bithon_" + name.replace("-", "_");
    }

    default String getWriteTableName(String name) {
        return "bithon_" + name.replace("-", "_");
    }
}
