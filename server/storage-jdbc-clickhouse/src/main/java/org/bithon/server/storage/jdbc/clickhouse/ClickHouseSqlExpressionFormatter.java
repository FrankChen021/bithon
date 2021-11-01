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

package org.bithon.server.storage.jdbc.clickhouse;

import org.bithon.agent.core.utils.lang.StringUtils;
import org.bithon.server.common.utils.datetime.TimeSpan;
import org.bithon.server.storage.jdbc.metric.ISqlExpressionFormatter;

/**
 * @author Frank Chen
 * @date 1/11/21 5:21 pm
 */
class ClickHouseSqlExpressionFormatter implements ISqlExpressionFormatter {

    private final ClickHouseConfig config;

    ClickHouseSqlExpressionFormatter(ClickHouseConfig config) {
        this.config = config;
    }

    @Override
    public String timeFloor(String field, long interval) {
        return String.format("CAST(toUnixTimestamp(\"%s\")/ %d AS Int64) * %d", field, interval, interval);
    }

    @Override
    public boolean groupByUseRawExpression() {
        return false;
    }

    /**
     * ClickHouse does not support ISO8601 very well, we treat it as timestamp, which only accepts timestamp in seconds not milli-seconds
     */
    @Override
    public String formatTimestamp(TimeSpan timeSpan) {
        return String.format("fromUnixTimestamp(%d)", timeSpan.getMilliseconds() / 1000);
    }

    @Override
    public String orderByTimestamp(String timestampField) {
        return "ORDER BY \"" + timestampField + "\"";
    }

    @Override
    public String getReadTableName(String name) {
        return StringUtils.isBlank(config.getCluster()) ? ISqlExpressionFormatter.super.getReadTableName(name)
                                                        : ISqlExpressionFormatter.super.getReadTableName(name) + "_all";
    }

    @Override
    public String getWriteTableName(String name) {
        return StringUtils.isBlank(config.getCluster()) ? ISqlExpressionFormatter.super.getReadTableName(name)
                                                        : ISqlExpressionFormatter.super.getReadTableName(name) + "_local";
    }
}
