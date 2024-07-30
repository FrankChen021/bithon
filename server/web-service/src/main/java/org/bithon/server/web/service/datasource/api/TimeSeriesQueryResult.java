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

package org.bithon.server.web.service.datasource.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bithon.server.commons.time.TimeSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/16 14:42
 */
@Data
@AllArgsConstructor
public class TimeSeriesQueryResult {
    /**
     * How many data points for one series
     */
    private final int count;

    /**
     * The aligned start timestamp that can be seen as: toStartOfTime(start, INTERVAL intervalSecond SECOND).
     * Inclusive
     */
    private final long startTimestamp;

    /**
     * The aligned end timestamp that can be seen as: toStartOfTime(end, INTERVAL intervalSecond SECOND).
     * Inclusive
     */
    private final long endTimestamp;

    /**
     * in milliseconds
     */
    private final long interval;
    private final Collection<TimeSeriesMetric> metrics;

    public static TimeSeriesQueryResult build(TimeSpan start,
                                              TimeSpan end,
                                              long intervalSecond,
                                              List<Map<String, Object>> dataPoints,
                                              String tsColumn,
                                              List<String> groups,
                                              List<String> metrics) {

        // For example, if the end timestamp is 02:00 (which falls at exactly the end of an interval),
        // it's NOT inclusive because we use the '<' comparison for the end timestamp.
        //
        // However, if the end timestamp is 02:03,
        // it will be floored down to 02:00, and this 02:00 is included in the result
        boolean isEndInclusive = end.toSeconds() % intervalSecond != 0;

        long startSecond = start.toSeconds() / intervalSecond * intervalSecond;
        long endSecond = end.toSeconds() / intervalSecond * intervalSecond;
        int bucketCount = (int) ((endSecond - startSecond) / intervalSecond + (isEndInclusive ? 1 : 0));

        // Use LinkedHashMap to retain the order of input metric list
        Map<List<String>, TimeSeriesMetric> map = new LinkedHashMap<>(7);

        if (dataPoints.isEmpty()) {
            // fill empty data points
            for (String metric : metrics) {
                List<String> tags = Collections.singletonList(metric);

                map.computeIfAbsent(tags, v -> new TimeSeriesMetric(tags, bucketCount));
            }
        } else {
            for (Map<String, Object> point : dataPoints) {
                long timestamp = ((Number) point.get(tsColumn)).longValue();
                int bucketIndex = (int) ((timestamp - startSecond) / intervalSecond);

                for (String metric : metrics) {
                    // this code is not so efficient
                    // we can wrap the point object to get the key and deserialize the wrap object directly
                    List<String> tags = new ArrayList<>();
                    for (String group : groups) {
                        tags.add((String) point.get(group));
                    }
                    tags.add(metric);

                    map.computeIfAbsent(tags, v -> new TimeSeriesMetric(tags, bucketCount))
                       .set(bucketIndex, point.get(metric));
                }
            }
        }

        return new TimeSeriesQueryResult(bucketCount,
                                         startSecond * 1000,
                                         // Since the returned end timestamp is always inclusive,
                                         // we need to check if the current got end timestamp is inclusive.
                                         // If not, we need to shift an interval to make it inclusive.
                                         isEndInclusive ? endSecond * 1000 : (endSecond - intervalSecond) * 1000,
                                         intervalSecond * 1000L,
                                         map.values());
    }
}
