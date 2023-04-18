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

package org.bithon.server.storage.jdbc.utils;

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpressions;

/**
 * @author Frank Chen
 * @date 17/4/23 11:20 pm
 */
public class H2SqlDialect implements ISqlDialect {
    public static final ISqlDialect INSTANCE = new H2SqlDialect();

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
        return StringUtils.format("group_concat(\"%s\")", field);
    }

    @Override
    public String firstAggregator(String field, String name, long window) {
        return StringUtils.format(
                "FIRST_VALUE(\"%s\") OVER (partition by %s ORDER BY \"timestamp\") AS \"%s\"",
                field,
                this.timeFloor("timestamp", window),
                name);
    }

    @Override
    public String lastAggregator(String field, long window) {
        // NOTE: use FIRST_VALUE since LAST_VALUE returns wrong result
        return StringUtils.format(
                "FIRST_VALUE(\"%s\") OVER (partition by %s ORDER BY \"timestamp\" DESC)",
                field,
                this.timeFloor("timestamp", window));
    }

    @Override
    public boolean useWindowFunctionAsAggregator(String aggregator) {
        return SimpleAggregateExpressions.FirstAggregateExpression.TYPE.equals(aggregator)
                || SimpleAggregateExpressions.LastAggregateExpression.TYPE.equals(aggregator);
    }

    /*
     * NOTE, H2 does not support timestamp comparison, we have to use ISO8601 format
     */
}
