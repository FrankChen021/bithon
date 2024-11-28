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

package org.bithon.server.metric.expression.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.bithon.component.commons.Experimental;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.metric.expression.MetricExpression;
import org.bithon.server.metric.expression.MetricExpressionASTBuilder;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.bithon.server.web.service.datasource.api.TimeSeriesMetric;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 26/11/24 2:36 pm
 */
@CrossOrigin
@RestController
public class MetricQueryApi {
    private final IDataSourceApi dataSourceApi;
    private final ExecutorService executor;

    public MetricQueryApi(IDataSourceApi dataSourceApi) {
        this.dataSourceApi = dataSourceApi;
        this.executor = Executors.newCachedThreadPool(NamedThreadFactory.nonDaemonThreadFactory("metric-query"));
    }

    @Data
    public static class MetricQueryRequest {
        @NotBlank
        private String expression;

        @NotNull
        @Valid
        private IntervalRequest interval;
    }

    @Experimental
    @PostMapping("/api/metric/timeseries")
    public QueryResponse timeSeries(@Validated @RequestBody MetricQueryRequest request) throws Exception {
        MetricExpression metricExpression = MetricExpressionASTBuilder.parse(request.getExpression());

        if (metricExpression.getOffset() != null) {
            CountDownLatch latch = new CountDownLatch(2);
            Future<QueryResponse> current = this.executor.submit(() -> {
                QueryRequest req = QueryRequest.builder()
                                               .dataSource(metricExpression.getFrom())
                                               .filterExpression(metricExpression.getWhereText())
                                               .groupBy(metricExpression.getGroupBy())
                                               .fields(List.of(metricExpression.getMetric()))
                                               .interval(request.getInterval())
                                               .build();
                try {
                    return dataSourceApi.timeseriesV4(req);
                } finally {
                    latch.countDown();
                }
            });

            Future<QueryResponse> base = this.executor.submit(() -> {
                // Offset expression is negative
                long seconds = -metricExpression.getOffset().getDuration().getSeconds();
                QueryRequest req = QueryRequest.builder()
                                               .dataSource(metricExpression.getFrom())
                                               .filterExpression(metricExpression.getWhereText())
                                               .groupBy(metricExpression.getGroupBy())
                                               .fields(List.of(metricExpression.getMetric()))
                                               .interval(IntervalRequest.builder()
                                                                        .startISO8601(TimeSpan.fromISO8601(request.getInterval().getStartISO8601()).before(seconds, TimeUnit.SECONDS).toISO8601())
                                                                        .endISO8601(TimeSpan.fromISO8601(request.getInterval().getEndISO8601()).before(seconds, TimeUnit.SECONDS).toISO8601())
                                                                        .bucketCount(request.getInterval().getBucketCount())
                                                                        .step(request.getInterval().getStep())
                                                                        .timestampColumn(request.getInterval().getTimestampColumn())
                                                                        .build())
                                               .build();
                try {
                    return dataSourceApi.timeseriesV4(req);
                } finally {
                    latch.countDown();
                }
            });

            latch.await();
            QueryResponse currentResponse = current.get();
            QueryResponse baseResponse = base.get();
            return merge(metricExpression.getExpected().getValue() instanceof HumanReadablePercentage,
                         metricExpression.getOffset(),
                         baseResponse,
                         currentResponse);
        } else {
            QueryRequest req = QueryRequest.builder()
                                           .dataSource(metricExpression.getFrom())
                                           .filterExpression(metricExpression.getWhereText())
                                           .groupBy(metricExpression.getGroupBy())
                                           .fields(List.of(metricExpression.getMetric()))
                                           .interval(request.getInterval())
                                           .build();
            return dataSourceApi.timeseriesV4(req);
        }
    }

    private QueryResponse merge(boolean usePercentage,
                                HumanReadableDuration offset,
                                QueryResponse baseResponse,
                                QueryResponse currentResponse) {
        TimeSeriesMetric base = (TimeSeriesMetric) baseResponse.getData().iterator().next();
        base.getTags().add(0, offset.toString());

        TimeSeriesMetric curr = (TimeSeriesMetric) currentResponse.getData().iterator().next();
        curr.getTags().add(0, "curr");

        // Calculate delta
        TimeSeriesMetric delta = new TimeSeriesMetric(List.of("delta", "delta"), base.getValues().length);
        for (long start = baseResponse.getStartTimestamp(); start <= baseResponse.getEndTimestamp(); start += baseResponse.getInterval()) {
            int index = (int) ((start - baseResponse.getStartTimestamp()) / baseResponse.getInterval());

            BigDecimal diff = BigDecimal.valueOf(curr.getValues()[index] - base.getValues()[index]);
            if (usePercentage) {
                delta.set(index, base.getValues()[index] == 0 ? 0 : diff.divide(BigDecimal.valueOf(base.getValues()[index]), 4, RoundingMode.HALF_UP).doubleValue());
            } else {
                delta.set(index, diff);
            }
        }

        return QueryResponse.builder()
                            .interval(currentResponse.getInterval())
                            .startTimestamp(currentResponse.getStartTimestamp())
                            .endTimestamp(currentResponse.getEndTimestamp())
                            .data(List.of(delta, base, curr))
                            .build();
    }
}
