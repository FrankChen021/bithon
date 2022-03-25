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

import org.bithon.server.common.utils.datetime.TimeSpan;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.storage.IFilter;
import org.bithon.server.metric.storage.IMetricStorage;
import org.bithon.server.metric.storage.Interval;
import org.bithon.server.metric.storage.TimeseriesQuery;
import org.bithon.server.tracing.TraceDataSourceSchema;
import org.bithon.server.tracing.sink.TraceSpan;
import org.bithon.server.tracing.storage.ITraceReader;
import org.bithon.server.tracing.storage.ITraceStorage;
import org.bithon.server.web.service.tracing.api.GetTraceDistributionResponse;
import org.bithon.server.web.service.tracing.api.TraceMap;
import org.bithon.server.web.service.tracing.api.TraceSpanBo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 24/11/21 7:11 pm
 */
@Service
public class TraceService {

    private final ITraceReader traceReader;
    private final IMetricStorage metricStorage;

    public TraceService(ITraceStorage traceStorage, IMetricStorage metricStorage) {
        this.traceReader = traceStorage.createReader();
        this.metricStorage = metricStorage;
    }

    /**
     * Requirement
     * 1. At most 60 buckets
     * 2. The minimal length of each bucket is 1 minute
     * <p>
     *
     * @param startTimestamp in millisecond
     * @param endTimestamp   in millisecond
     * @return the length of a bucket in second
     */
    static Bucket getTimeBucket(long startTimestamp, long endTimestamp) {
        int seconds = (int) ((endTimestamp - startTimestamp) / 1000);
        if (seconds <= 60) {
            return new Bucket(12, 5);
        }

        int minute = (int) ((endTimestamp - startTimestamp) / 1000 / 60);
        int hour = (int) Math.ceil(minute / 60.0);

        // after 3 hour, the step is 6 hour
        int step = hour <= 3 ? 1 : 6;

        int m = hour % step;
        int hourPerStep = (hour / step + (m > 0 ? 1 : 0));

        int length = hourPerStep * step * 3600 / 60; // the last 60 is the max bucket number
        int mod = seconds % length;
        return new Bucket(seconds / length + (mod > 0 ? 1 : 0), length);
    }

    public TraceMap buildMap(List<TraceSpan> spans) {
        return new TraceMapBuilder().buildMap(spans);
    }

    public List<TraceSpan> getTraceByParentSpanId(String parentSpanId) {
        return traceReader.getTraceByParentSpanId(parentSpanId);
    }

    public List<TraceSpan> getTraceByTraceId(String txId,
                                             String type,
                                             String startTimeISO8601,
                                             String endTimeISO8601,
                                             boolean asTree) {
        if (!"trace".equals(type)) {
            // check if the id has a user mapping
            String traceId = traceReader.getTraceIdByMapping(txId);
            if (traceId != null) {
                txId = traceId;
            }
            // if there's no mapping, try to search this id as trace id
        }

        TimeSpan start = StringUtils.hasText(startTimeISO8601) ? TimeSpan.fromISO8601(startTimeISO8601) : null;
        TimeSpan end = StringUtils.hasText(endTimeISO8601) ? TimeSpan.fromISO8601(endTimeISO8601) : null;
        List<TraceSpan> spans = traceReader.getTraceByTraceId(txId, start, end);

        if (!asTree) {
            return spans;
        }

        //
        // build as tree
        //
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
                    // the root span of B has parentSpanId but of course has no parent span
                    rootSpans.add(bo);
                } else {
                    parentSpan.children.add(bo);
                }
            }
        }
        return rootSpans;
    }

    public int getTraceListSize(List<IFilter> filters, Timestamp start, Timestamp end) {
        return traceReader.getTraceListSize(filters, start, end);
    }

    public List<TraceSpan> getTraceList(List<IFilter> filters,
                                        Timestamp start,
                                        Timestamp end,
                                        String orderBy,
                                        String order,
                                        int pageNumber,
                                        int pageSize) {
        return traceReader.getTraceList(filters,
                                        start,
                                        end,
                                        orderBy,
                                        order,
                                        pageNumber, pageSize);
    }

    @Deprecated
    public GetTraceDistributionResponse getTraceDistribution(List<IFilter> filters,
                                                             String startTimeISO8601,
                                                             String endTimeISO8601) {
        TimeSpan start = TimeSpan.fromISO8601(startTimeISO8601);
        TimeSpan end = TimeSpan.fromISO8601(endTimeISO8601);
        Interval interval = Interval.of(start,
                                        end,
                                        getTimeBucket(start.getMilliseconds(), end.getMilliseconds()).getLength());

        // create a virtual data source to use current metric API to query
        DataSourceSchema schema = TraceDataSourceSchema.getTraceSpanSchema();

        TimeseriesQuery query = new TimeseriesQuery(schema,
                                                    Collections.singletonList("count"),
                                                    filters,
                                                    interval,
                                                    Collections.emptyList());

        // TODO 2：Provide a custom query
        // OR implement a new interface
        return new GetTraceDistributionResponse(metricStorage.createMetricReader(schema).timeseries(query),
                                                interval.getStepLength());
    }


    @Deprecated
    public List<ITraceReader.Histogram> getTraceDistributionV2(List<IFilter> filters,
                                                               TimeSpan start,
                                                               TimeSpan end) {
        return traceReader.getTraceDistribution(filters, start.toTimestamp(), end.toTimestamp());
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
