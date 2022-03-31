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

package org.bithon.server.web.service.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.metric.aggregator.spec.IMetricSpec;
import org.bithon.server.metric.typing.DoubleValueType;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.TimeseriesQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author Frank Chen
 * @date 8/1/22 9:57 PM
 */
@Service
public class DataSourceService {

    private static final String TIMESTAMP_QUERY_NAME = "_timestamp";
    private final IMetricStorage metricStorage;

    public DataSourceService(IMetricStorage metricStorage) {
        this.metricStorage = metricStorage;
    }

    /**
     * old interface
     */
    public List<Map<String, Object>> oldTimeseriesQuery(TimeseriesQuery query) {
        List<Map<String, Object>> queryResult = this.metricStorage.createMetricReader(query.getDataSource())
                                                                  .timeseries(query);

        //
        // fill empty time slot bucket
        //
        List<Map<String, Object>> returns = new ArrayList<>();
        int j = 0;
        TimeSpan start = query.getInterval().getStartTime();
        TimeSpan end = query.getInterval().getEndTime();
        int step = query.getInterval().getStepLength();
        for (long bucket = start.toSeconds() / step * step, endBucket = end.toSeconds() / step * step;
             bucket < endBucket;
             bucket += step) {
            if (j < queryResult.size()) {
                long nextSlot = ((Number) queryResult.get(j).get(TIMESTAMP_QUERY_NAME)).longValue();
                while (bucket < nextSlot) {
                    Map<String, Object> empty = new HashMap<>(query.getMetrics().size());
                    empty.put(TIMESTAMP_QUERY_NAME, bucket * 1000);
                    query.getMetrics().forEach((metric) -> empty.put(metric, 0));
                    returns.add(empty);
                    bucket += step;
                }

                // convert to millisecond
                queryResult.get(j).put(TIMESTAMP_QUERY_NAME, nextSlot * 1000);
                returns.add(queryResult.get(j++));
            } else {
                Map<String, Object> empty = new HashMap<>(query.getMetrics().size());
                empty.put(TIMESTAMP_QUERY_NAME, bucket * 1000);
                query.getMetrics().forEach((metric) -> empty.put(metric, 0));
                returns.add(empty);
            }
        }
        while (j < queryResult.size()) {
            queryResult.get(j).put(TIMESTAMP_QUERY_NAME, ((Number) queryResult.get(j).get(TIMESTAMP_QUERY_NAME)).longValue() * 1000L);
            returns.add(queryResult.get(j++));
        }
        return returns;
    }

    /**
     * a more friendly interface for the front-end, and also size saving
     *
     * @return key - the name of a metric
     * val - the time series values
     */
    public TimeSeriesQueryResult timeseriesQuery(TimeseriesQuery query) {
        List<Map<String, Object>> points = this.metricStorage.createMetricReader(query.getDataSource())
                                                             .timeseries(query);

        TimeSpan start = query.getInterval().getStartTime();
        TimeSpan end = query.getInterval().getEndTime();
        int step = query.getInterval().getStepLength();
        long startSecond = start.toSeconds() / step * step;
        long endSecond = end.toSeconds() / step * step;
        int bucketCount = (int) (endSecond - startSecond) / step;

        Map<List<String>, TimeSeriesMetric> map = new HashMap<>();

        for (Map<String, Object> point : points) {
            long timestamp = ((Number) point.get(TIMESTAMP_QUERY_NAME)).longValue();
            int bucketIndex = (int) (timestamp - startSecond) / step;

            for (String metric : query.getMetrics()) {
                // this code is not so efficient
                // we can wrap the point object to get the key and deserialize the wrap object directly
                List<String> tags = new ArrayList<>();
                for (String group : query.getGroupBys()) {
                    tags.add((String) point.get(group));
                }
                tags.add(metric);

                map.computeIfAbsent(tags,
                                    v -> new TimeSeriesMetric(tags,
                                                              bucketCount,
                                                              query.getDataSource().getMetricSpecByName(metric)))
                   .set(bucketIndex, point.get(metric));
            }
        }

        TimeSeriesQueryResult result = new TimeSeriesQueryResult();
        result.interval = step * 1000L;
        result.startTimestamp = startSecond * 1000;
        result.endTimestamp = endSecond * 1000;
        result.count = bucketCount;
        result.metrics = map.values();

        return result;
    }

    @Data
    public static class TimeSeriesMetric {
        private List<String> tags;
        private Object values;

        @JsonIgnore
        private BiConsumer<Integer, Object> valueSetter;

        public TimeSeriesMetric(List<String> tags, int size, IMetricSpec metricSpec) {
            this.tags = tags;

            // by using double[] or long[], the empty slots are default to zero
            if (metricSpec.getValueType() instanceof DoubleValueType) {
                this.values = new double[size + 1];
                this.valueSetter = (index, number) -> ((double[]) values)[index] = number == null ? 0 : ((Number) number).doubleValue();
            } else {
                this.values = new long[size + 1];
                this.valueSetter = (index, number) -> ((long[]) values)[index] = number == null ? 0 : ((Number) number).longValue();
            }
        }

        public void set(int index, Object value) {
            this.valueSetter.accept(index, value);
        }
    }

    @Data
    public static class TimeSeriesQueryResult {
        private int count;
        private long startTimestamp;
        private long endTimestamp;
        private long interval;
        private Collection<TimeSeriesMetric> metrics;
    }
}
