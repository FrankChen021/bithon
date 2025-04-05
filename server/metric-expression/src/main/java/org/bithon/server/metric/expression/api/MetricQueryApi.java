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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.component.commons.Experimental;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.server.metric.expression.ast.MetricExpression;
import org.bithon.server.metric.expression.ast.MetricExpressionASTBuilder;
import org.bithon.server.metric.expression.evaluation.EvaluatorBuilder;
import org.bithon.server.metric.expression.evaluation.IEvaluator;
import org.bithon.server.metric.expression.evaluation.mutation.FillEmptyBucketMutation;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.bithon.server.web.service.datasource.api.TimeSeriesMetric;
import org.springframework.context.annotation.Conditional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 26/11/24 2:36 pm
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class MetricQueryApi {
    private final IDataSourceApi dataSourceApi;
    private final ExecutorService executor;

    public MetricQueryApi(IDataSourceApi dataSourceApi) {
        this.dataSourceApi = dataSourceApi;
        this.executor = Executors.newCachedThreadPool(NamedThreadFactory.nonDaemonThreadFactory("metric-query"));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricQueryRequest {
        /**
         * Metric QL expression such as "sum(cpu_usage) by host"
         */
        @NotBlank
        private String expression;

        /**
         * Extra condition
         */
        @Nullable
        private String condition;

        @NotNull
        @Valid
        private IntervalRequest interval;
    }

    @Experimental
    @PostMapping("/api/metric/timeseries")
    public QueryResponse timeSeries(@Validated @RequestBody MetricQueryRequest request) throws Exception {
        IExpression expression = MetricExpressionASTBuilder.parse(request.getExpression());
        if (!(expression instanceof MetricExpression metricExpression)) {
            throw new IllegalArgumentException("Invalid metric expression: " + request.getExpression());
        }

        String filterExpression = Stream.of(metricExpression.getWhereText(), request.getCondition())
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.joining(" AND "));

        if (metricExpression.getOffset() != null) {
            CountDownLatch latch = new CountDownLatch(2);
            Future<QueryResponse> current = this.executor.submit(() -> {
                QueryRequest req = QueryRequest.builder()
                                               .dataSource(metricExpression.getFrom())
                                               .filterExpression(filterExpression)
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
                QueryRequest req = QueryRequest.builder()
                                               .dataSource(metricExpression.getFrom())
                                               .filterExpression(filterExpression)
                                               .groupBy(metricExpression.getGroupBy())
                                               .fields(List.of(metricExpression.getMetric()))
                                               .offset(metricExpression.getOffset())
                                               .interval(request.getInterval())
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
                                           .filterExpression(filterExpression)
                                           .groupBy(metricExpression.getGroupBy())
                                           .fields(List.of(metricExpression.getMetric()))
                                           .interval(request.getInterval())
                                           .build();
            return dataSourceApi.timeseriesV4(req);
        }
    }

    @Experimental
    @PostMapping("/api/metric/timeseries/v2")
    public QueryResponse timeSeriesV2(@Validated @RequestBody MetricQueryRequest request) throws Exception {
        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(request.getInterval())
                                               .condition(request.getCondition())
                                               .build(request.getExpression());

        return new FillEmptyBucketMutation(evaluator).evaluate();
    }

    private QueryResponse<?> merge(boolean usePercentage,
                                   HumanReadableDuration offset,
                                   QueryResponse<Collection<TimeSeriesMetric>> baseResponse,
                                   QueryResponse<Collection<TimeSeriesMetric>> currentResponse) {
        TimeSeriesMetric base = baseResponse.getData().iterator().next();
        base.getTags().add(0, offset.toString());

        TimeSeriesMetric curr = currentResponse.getData().iterator().next();
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
