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

package org.bithon.server.storage.druid;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.jdbc.utils.ISqlDialect;

/**
 * @author frank.chen021@outlook.com
 * @date 1/11/21 5:21 pm
 */
@JsonTypeName("DRUID")
public class DruidSqlDialect implements ISqlDialect {

    @Override
    public String timeFloorExpression(String field, long interval) {
        return StringUtils.format("CAST(toUnixTimestamp(\"%s\")/ %d AS Int64) * %d", field, interval, interval);
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
     * ClickHouse does not support ISO8601 very well; we treat it as timestamp, which only accepts timestamp in seconds not milliseconds
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
