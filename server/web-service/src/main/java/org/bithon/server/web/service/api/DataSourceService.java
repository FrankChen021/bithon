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
import org.bithon.server.storage.datasource.api.IQueryStageAggregator;
import org.bithon.server.storage.datasource.spec.IMetricSpec;
import org.bithon.server.storage.datasource.typing.DoubleValueType;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.TimeseriesQuery;
import org.bithon.server.storage.metrics.TimeseriesQueryV2;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 8/1/22 9:57 PM
 */
@Service
public class DataSourceService {

    private static final String TIMESTAMP_QUERY_NAME = "_timestamp";
    private final IMetricStorage metricStorage;

    public DataSourceService(IMetricStorage metricStorage) {
        this.metricStorage = metricStorage;
    }

    public List<Map<String, Object>> oldTimeseriesQuery(TimeseriesQuery query) {
        List<Map<String, Object>> queryResult = this.metricStorage.createMetricReader(query.getDataSource())
                                                                  .timeseries(TimeseriesQueryV2.builder()
                                                                                               .dataSource(query.getDataSource())
                                                                                               .metrics(query.getMetrics())
                                                                                               .aggregators(Collections.emptyList())
                                                                                               .interval(query.getInterval())
                                                                                               .groupBys(query.getGroupBys())
                                                                                               .filters(query.getFilters())
                                                                                               .build());

        //
        // fill empty time slot bucket
        //
        List<Map<String, Object>> returns = new ArrayList<>();
        int j = 0;
        TimeSpan start = query.getInterval().getStartTime();
        TimeSpan end = query.getInterval().getEndTime();
        int step = query.getInterval().getStep();
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
     * @return Map<Tags, Vals>
     * Tags - dimension of a series
     * Vals - an array of all data points. Each element represents a data point of a timestamp.
     */
    public TimeSeriesQueryResult timeseriesQuery(TimeseriesQueryV2 query) {
        Set<String> metrics = query.getAggregators().stream().map(IQueryStageAggregator::getName).collect(Collectors.toSet());
        metrics.addAll(query.getMetrics());

        List<Map<String, Object>> points = this.metricStorage.createMetricReader(query.getDataSource())
                                                             .timeseries(query);

        TimeSpan start = query.getInterval().getStartTime();
        TimeSpan end = query.getInterval().getEndTime();
        int step = query.getInterval().getStep();
        long startSecond = start.toSeconds() / step * step;
        long endSecond = end.toSeconds() / step * step;
        int bucketCount = (int) (endSecond - startSecond) / step;

        Map<List<String>, TimeSeriesMetric> map = new HashMap<>(query.getAggregators().size());

        if (points.isEmpty()) {
            // fill empty data points
            for (String metric : metrics) {
                List<String> tags = Collections.singletonList(metric);

                map.computeIfAbsent(tags,
                                    v -> new TimeSeriesMetric(tags,
                                                              bucketCount,
                                                              query.getDataSource().getMetricSpecByName(metric)));
            }
        } else {
            for (Map<String, Object> point : points) {
                long timestamp = ((Number) point.get(TIMESTAMP_QUERY_NAME)).longValue();
                int bucketIndex = (int) (timestamp - startSecond) / step;

                for (String metric : metrics) {
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
        private final List<String> tags;

        /**
         * Actual type of values is either double or long.
         * {@link java.lang.Number} is not used because if an element is not set, the serialized value is null.
         * Since we want to keep the serialized value to be 0, the raw number type is the best
         */
        private final Object values;

        @JsonIgnore
        private BiConsumer<Integer, Object> valueSetter;

        @JsonIgnore
        private Function<Integer, Number> valueGetter;

        public TimeSeriesMetric(List<String> tags, int size, IMetricSpec metricSpec) {
            this.tags = tags;

            // by using double[] or long[], the empty slots are default to zero
            if (metricSpec.getValueType() instanceof DoubleValueType) {
                this.values = new double[size + 1];
                this.valueSetter = (index, number) -> ((double[]) values)[index] = number == null ? 0 : ((Number) number).doubleValue();
                this.valueGetter = (index) -> ((double[]) values)[index];
            } else {
                this.values = new long[size + 1];
                this.valueSetter = (index, number) -> ((long[]) values)[index] = number == null ? 0 : ((Number) number).longValue();
                this.valueGetter = (index) -> ((long[]) values)[index];
            }
        }

        public void set(int index, Object value) {
            this.valueSetter.accept(index, value);
        }

        public Number get(int index) {
            return this.valueGetter.apply(index);
        }
    }

    @Data
    public static class TimeSeriesQueryResult {
        /**
         * how many data points for one series
         */
        private int count;
        private long startTimestamp;
        private long endTimestamp;

        /**
         * in milliseconds
         */
        private long interval;
        private Collection<TimeSeriesMetric> metrics;

        public List<String> getTimestampLabels(String dateTimeFormat) {
            SimpleDateFormat formatter = new SimpleDateFormat(dateTimeFormat, Locale.ENGLISH);
            List<String> labels = new ArrayList<>(this.count);

            long timestamp = startTimestamp;
            for (int i = 0; i < this.count; i++) {
                labels.add(formatter.format(new Date(timestamp)));
                timestamp += interval;
            }
            return labels;
        }
    }
}
