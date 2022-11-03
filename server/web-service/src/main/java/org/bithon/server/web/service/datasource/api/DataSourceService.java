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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.IColumnSpec;
import org.bithon.server.storage.datasource.query.IQueryStageAggregator;
import org.bithon.server.storage.datasource.query.QueryStageAggregators;
import org.bithon.server.storage.datasource.dimension.IDimensionSpec;
import org.bithon.server.storage.datasource.spec.IMetricSpec;
import org.bithon.server.storage.datasource.spec.PostAggregatorMetricSpec;
import org.bithon.server.storage.datasource.typing.DoubleValueType;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.storage.metrics.Query;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    /**
     * @return Map<Tags, Vals>
     * Tags - dimension of a series
     * Vals - an array of all data points. Each element represents a data point of a timestamp.
     */
    public TimeSeriesQueryResult timeseriesQuery(Query query) {
        List<String> metrics = query.getFields().stream()
                                    .map((f) -> {
                                        if (f instanceof IColumnSpec) {
                                            return ((IColumnSpec) f).getName();
                                        }
                                        if (f instanceof String) {
                                            return (String) f;
                                        }
                                        if (f instanceof IQueryStageAggregator) {
                                            return ((IQueryStageAggregator) f).getName();
                                        }
                                        throw new RuntimeException("");
                                    }).collect(Collectors.toList());
        List<Map<String, Object>> points = this.metricStorage.createMetricReader(query.getDataSource())
                                                             .timeseries(query);

        TimeSpan start = query.getInterval().getStartTime();
        TimeSpan end = query.getInterval().getEndTime();
        int step = query.getInterval().getStep();
        long startSecond = start.toSeconds() / step * step;
        long endSecond = end.toSeconds() / step * step;
        int bucketCount = (int) (endSecond - startSecond) / step;

        // Use LinkedHashMap to retain the order of input metric list
        Map<List<String>, TimeSeriesMetric> map = new LinkedHashMap<>(7);

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
                    for (String group : query.getGroupBy()) {
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

    public Query convertToQuery(DataSourceSchema schema, GeneralQueryRequest query, boolean bucketTimestamp) {
        Query.QueryBuilder builder = Query.builder();

        List<String> groupBy = new ArrayList<>(4);

        // Turn into internal objects(post aggregators...)
        List<Object> fields = new ArrayList<>(query.getFields().size());
        for (QueryField field : query.getFields()) {
            if (field.getExpression() != null) {
                // expression metric
                PostAggregatorMetricSpec post = new PostAggregatorMetricSpec(field.getName(),
                                                                             field.getName(),
                                                                             "",
                                                                             field.getExpression(),
                                                                             null,
                                                                             false);
                post.setOwner(schema);
                fields.add(post);
                continue;
            }

            if (field.getAggregator() != null) {
                fields.add(QueryStageAggregators.create(field.getAggregator(),
                                                        field.getName(),
                                                        field.getField()));
            } else {
                IColumnSpec columnSpec = schema.getColumnByName(field.getField());
                if (columnSpec == null) {
                    throw new RuntimeException(StringUtils.format("field [%s] does not exist.", field.getField()));
                }
                if (columnSpec instanceof IDimensionSpec) {
                    fields.add(columnSpec.getName());
                    groupBy.add(columnSpec.getName());
                } else {
                    if (columnSpec instanceof PostAggregatorMetricSpec) {
                        fields.add(columnSpec);
                    } else {
                        fields.add(((IMetricSpec) columnSpec).getQueryAggregator());
                    }
                }
            }
        }

        TimeSpan start = TimeSpan.fromISO8601(query.getInterval().getStartISO8601());
        TimeSpan end = TimeSpan.fromISO8601(query.getInterval().getEndISO8601());

        /**
         * For timeseries query, divide the timestamp by default step
         * For groupBy query, use the whole length of interval as step
         */
        long windowLength;
        if (bucketTimestamp) {
            windowLength = Interval.calculateDefaultStep(start, end);
        } else {
            /*
             * For Window functions, since the timestamp of records might cross two windows,
             * we need to make sure the record in the given time range has only one window.
             */
            windowLength = end.toSeconds() - start.toSeconds();
            while (start.getMilliseconds() / windowLength != end.getMilliseconds() / windowLength) {
                windowLength *= 2;
            }
        }

        return builder.groupBy(groupBy)
                      .fields(fields)
                      .dataSource(schema)
                      .filters(CollectionUtils.emptyOrOriginal(query.getFilters()))
                      .interval(Interval.of(start, end, (int) windowLength))
                      .orderBy(query.getOrderBy())
                      .limit(query.getLimit())
                      .resultFormat(query.getResultFormat() == null ? Query.ResultFormat.Object : query.getResultFormat())
                      .build();
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
