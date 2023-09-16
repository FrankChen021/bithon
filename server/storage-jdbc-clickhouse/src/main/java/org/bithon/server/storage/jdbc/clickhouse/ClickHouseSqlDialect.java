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

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.jdbc.utils.ISqlDialect;

/**
 * @author frank.chen021@outlook.com
 * @date 1/11/21 5:21 pm
 */
@JsonTypeName("CLICKHOUSE")
public class ClickHouseSqlDialect implements ISqlDialect {

    @Override
    public String timeFloorExpression(IExpression timestampExpression, long interval) {
        if (interval == 60L) {
            if (timestampExpression instanceof FunctionExpression) {
                if ("toStartOfMinute".equals(((FunctionExpression) timestampExpression).getName())) {
                    // If the user already specifies the toStartOfMinute function call,
                    // there's no need to apply the function once more
                    return StringUtils.format("toUnixTimestamp(%s)", timestampExpression.serializeToText(true));
                }
            }
            return StringUtils.format("toUnixTimestamp(toStartOfMinute(%s))", timestampExpression.serializeToText(true));
        }
        if (interval == 60 * 5) {
            return StringUtils.format("toUnixTimestamp(toStartOfFiveMinute(%s))", timestampExpression.serializeToText(true));
        }
        if (interval == 60 * 15) {
            return StringUtils.format("toUnixTimestamp(toStartOfFifteenMinutes(%s))", timestampExpression.serializeToText(true));
        }
        if (interval == 3600) {
            return StringUtils.format("toUnixTimestamp(toStartOfHour(%s))", timestampExpression.serializeToText(true));
        }

        if (interval > 60 && interval % 60 == 0) {
            return StringUtils.format("toUnixTimestamp(toStartOfInterval(%s, INTERVAL %d MINUTE))",
                                      timestampExpression.serializeToText(true),
                                      interval / 60);
        } else {
            return StringUtils.format("CAST(toUnixTimestamp(%s)/ %d AS Int64) * %d", timestampExpression.serializeToText(true), interval, interval);
        }
    }

    @Override
    public boolean groupByUseRawExpression() {
        return false;
    }

    @Override
    public boolean allowSameAggregatorExpression() {
        return false;
    }

    /**
     * ClickHouse does not support ISO8601 very well, we treat it as timestamp, which only accepts timestamp in seconds not milliseconds
     */
    @Override
    public String formatTimestamp(TimeSpan timeSpan) {
        return StringUtils.format("fromUnixTimestamp(%d)", timeSpan.getMilliseconds() / 1000);
    }

    @Override
    public String stringAggregator(String field) {
        return StringUtils.format("arrayStringConcat(arrayCompact(arrayFilter(x -> x <> '', groupArray(%s))), ',')",
                                  field);
    }

    @Override
    public String firstAggregator(String field, String name, long window) {
        return StringUtils.format("argMin(\"%s\", \"timestamp\") AS %s", field, name);
    }

    @Override
    public String lastAggregator(String field, long window) {
        return StringUtils.format("argMax(\"%s\", \"timestamp\")", field);
    }
}
