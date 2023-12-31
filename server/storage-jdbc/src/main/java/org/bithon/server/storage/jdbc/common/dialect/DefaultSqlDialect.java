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

/**
 * @author Frank Chen
 * @date 17/4/23 11:23 pm
 */
public class DefaultSqlDialect implements ISqlDialect {
    public static final ISqlDialect INSTANCE = new DefaultSqlDialect();

    @Override
    public boolean groupByUseRawExpression() {
        return false;
    }

    @Override
    public boolean allowSameAggregatorExpression() {
        return true;
    }

    @Override
    public String stringAggregator(String field) {
        throw new RuntimeException("string agg is not supported.");
    }

    @Override
    public String firstAggregator(String field, String name, long window) {
        throw new RuntimeException("last agg is not supported.");
    }

    @Override
    public String lastAggregator(String field, long window) {
        throw new RuntimeException("last agg is not supported.");
    }
}
