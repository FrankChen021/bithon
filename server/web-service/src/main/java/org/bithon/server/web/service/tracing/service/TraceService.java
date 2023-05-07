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

package org.bithon.server.web.service.tracing.service;

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.storage.tracing.ITraceReader;
import org.bithon.server.storage.tracing.ITraceStorage;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.bithon.server.storage.tracing.index.TagIndexConfig;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.datasource.api.TimeSeriesQueryResult;
import org.bithon.server.web.service.tracing.api.TraceSpanBo;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 24/11/21 7:11 pm
 */
@Service
@Conditional(value = WebServiceModuleEnabler.class)
public class TraceService {

    private final ITraceReader traceReader;
    private final TraceStorageConfig traceStorageConfig;

    public TraceService(ITraceStorage traceStorage,
                        TraceStorageConfig traceStorageConfig) {
        this.traceReader = traceStorage.createReader();
        this.traceStorageConfig = traceStorageConfig;
    }

    /**
     * For test only
     */
    static Bucket getTimeBucket(long startTimestamp, long endTimestamp) {
        return getTimeBucket(startTimestamp, endTimestamp, 60);
    }

    /**
     * Requirement
     * 1. The minimal length of each bucket is 1 minute
     *
     * <p>
     *
     * @param bucketCount    the count of buckets
     * @param startTimestamp in millisecond
     * @param endTimestamp   in millisecond
     * @return the length of a bucket in second
     */
    static Bucket getTimeBucket(long startTimestamp, long endTimestamp, int bucketCount) {
        int seconds = (int) ((endTimestamp - startTimestamp) / 1000);
        if (seconds <= 60) {
            return new Bucket(1, 60);
        }

        int minute = (int) ((endTimestamp - startTimestamp) / 1000 / 60);
        int hour = (int) Math.ceil(minute / 60.0);

        int[] steps = {1, 5, 10, 15, 30, 60, 150, 180, 360, 720, 1440};
        int stepIndex = 0;
        int step = 1;
        while (minute / step > bucketCount) {
            stepIndex++;
            if (stepIndex < steps.length) {
                step = steps[stepIndex];
            } else {
                // if exceeding the predefined steps, increase 1 day at least
                step += 1440;
            }
        }

        int m = hour % step;
        int hourPerStep = (hour / step + (m > 0 ? 1 : 0));

        // the last 60 is the max bucket number
        int length = hourPerStep * step * 3600 / 60;
        int mod = seconds % length;
        return new Bucket(seconds / length + (mod > 0 ? 1 : 0), length);
    }

    public List<TraceSpan> getTraceByParentSpanId(String parentSpanId) {
        return traceReader.getTraceByParentSpanId(parentSpanId);
    }

    public List<TraceSpan> getTraceByTraceId(String txId,
                                             String type,
                                             String startTimeISO8601,
                                             String endTimeISO8601) {
        TimeSpan start = StringUtils.hasText(startTimeISO8601) ? TimeSpan.fromISO8601(startTimeISO8601) : null;
        TimeSpan end = StringUtils.hasText(endTimeISO8601) ? TimeSpan.fromISO8601(endTimeISO8601) : null;

        if (!"trace".equals(type)) {
            // check if the id has a user mapping
            TraceIdMapping mapping = traceReader.getTraceIdByMapping(txId);
            if (mapping != null) {
                txId = mapping.getTraceId();

                // Set the time range to narrow down the search range
                if (start == null) {
                    start = TimeSpan.of(mapping.getTimestamp() - 2 * 3600 * 1000L);
                }
                if (end == null) {
                    end = TimeSpan.of(mapping.getTimestamp() + 2 * 3600 * 1000L);
                }
            }
            // if there's no mapping, try to search this id as trace id
        }

        return traceReader.getTraceByTraceId(txId, start, end);
    }

    public List<TraceSpan> asTree(List<TraceSpan> spans) {
        Map<String, TraceSpanBo> boMap = spans.stream().collect(Collectors.toMap(span -> span.spanId, val -> {
            TraceSpanBo bo = new TraceSpanBo();
            BeanUtils.copyProperties(val, bo);
            return bo;
        }));

        List<TraceSpan> rootSpans = new ArrayList<>();
        for (TraceSpan span : spans) {
            TraceSpanBo bo = boMap.get(span.spanId);
            if (StringUtils.isEmpty(span.parentSpanId)) {
                rootSpans.add(bo);
            } else {
                TraceSpanBo parentSpan = boMap.get(span.parentSpanId);
                if (parentSpan == null) {
                    // For example, two applications: A --> B
                    // if span logs of A are not stored in Bithon,
                    // the root span of B has parentSpanId, but apparently the parent span can't be found
                    rootSpans.add(bo);
                } else {
                    parentSpan.children.add(bo);
                }
            }
        }
        return rootSpans;
    }

    public int getTraceListSize(List<IFilter> filters,
                                String filterExpression,
                                Timestamp start,
                                Timestamp end) {
        // Convert filter expression into filter objects first
        filters.addAll(FilterExpressionToFilters.toFilter(filterExpression));

        FilterSplitter splitter = new FilterSplitter(this.traceStorageConfig);
        splitter.split(filters);

        return traceReader.getTraceListSize(splitter.filters, splitter.indexedTagFilter, splitter.nonIndexedTagFilter, start, end);
    }

    public List<TraceSpan> getTraceList(List<IFilter> filters,
                                        String filterExpression,
                                        Timestamp start,
                                        Timestamp end,
                                        String orderBy,
                                        String order,
                                        int pageNumber,
                                        int pageSize) {
        // Convert filter expression into filter objects first
        filters.addAll(FilterExpressionToFilters.toFilter(filterExpression));

        FilterSplitter splitter = new FilterSplitter(this.traceStorageConfig);
        splitter.split(filters);

        return traceReader.getTraceList(splitter.filters,
                                        splitter.indexedTagFilter,
                                        splitter.nonIndexedTagFilter,
                                        start,
                                        end,
                                        orderBy,
                                        order,
                                        pageNumber, pageSize);
    }

    public TimeSeriesQueryResult getTraceDistribution(List<IFilter> filters,
                                                      String filterExpression,
                                                      TimeSpan start,
                                                      TimeSpan end,
                                                      int bucketCount) {
        // Convert filter expression into filter objects first
        filters.addAll(FilterExpressionToFilters.toFilter(filterExpression));

        FilterSplitter splitter = new FilterSplitter(this.traceStorageConfig);
        splitter.split(filters);

        int bucketInSecond = getTimeBucket(start.getMilliseconds(), end.getMilliseconds(), bucketCount).length;
        List<Map<String, Object>> dataPoints = traceReader.getTraceDistribution(splitter.filters,
                                                                                splitter.indexedTagFilter,
                                                                                splitter.nonIndexedTagFilter,
                                                                                start.toTimestamp(),
                                                                                end.toTimestamp(),
                                                                                bucketInSecond);
        List<String> metrics = Arrays.asList("count", "minResponse", "avgResponse", "maxResponse");
        return TimeSeriesQueryResult.build(start, end, bucketInSecond, dataPoints, "_timestamp", Collections.emptyList(), metrics);
    }

    static class FilterSplitter {
        private final List<IFilter> filters;
        private final Map<Integer, IFilter> indexedTagFilter;
        private final List<IFilter> nonIndexedTagFilter;
        private final TraceStorageConfig traceStorageConfig;

        public FilterSplitter(TraceStorageConfig traceStorageConfig) {
            this.traceStorageConfig = traceStorageConfig;
            this.filters = new ArrayList<>();
            this.indexedTagFilter = new TreeMap<>();
            this.nonIndexedTagFilter = new ArrayList<>();
        }

        void split(List<IFilter> filters) {
            TagIndexConfig indexedTagConfig = this.traceStorageConfig.getIndexes();

            for (IFilter filter : filters) {
                if (!filter.getName().startsWith("tags.")) {
                    this.filters.add(filter);
                    continue;
                }

                String tagName = filter.getName().substring("tags".length());
                if (!StringUtils.hasText(tagName)) {
                    throw new RuntimeException(StringUtils.format("Wrong tag name [%s]", filter.getName()));
                }

                int tagIndex = indexedTagConfig == null ? 0 : indexedTagConfig.getColumnPos(tagName);
                if (tagIndex > 0) {
                    indexedTagFilter.put(tagIndex, filter);
                } else {
                    nonIndexedTagFilter.add(filter);
                }
            }
        }
    }


    static class Bucket {
        /**
         * number of buckets
         */
        final int nums;

        /**
         * length of bucket in second
         */
        final int length;

        public Bucket(int nums, int length) {
            this.nums = nums;
            this.length = length;
        }

        public int getNums() {
            return nums;
        }

        public int getLength() {
            return length;
        }
    }
}
