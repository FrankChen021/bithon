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

package org.bithon.server.alerting.common.evaluator.metric.relative;

import lombok.Getter;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.component.commons.utils.NumberUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutput;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryField;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author frankchen
 * @date 2020-08-21 17:06:34
 */
public abstract class AbstractRelativeThresholdPredicate implements IMetricEvaluator {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Getter
    private final long offset;

    protected final Number threshold;

    public AbstractRelativeThresholdPredicate(Number threshold,
                                              HumanReadableDuration offset) {
        Preconditions.checkArgumentNotNull("offset", offset);

        // duration is a negative value, we turn it into a positive one
        this.offset = -offset.getDuration().getSeconds();

        this.threshold = Preconditions.checkArgumentNotNull("threshold", threshold);
    }

    @Override
    public EvaluationOutputs evaluate(IDataSourceApi dataSourceApi,
                                      String dataSource,
                                      QueryField metric,
                                      TimeSpan start,
                                      TimeSpan end,
                                      String filterExpression,
                                      Set<String> groupBy,
                                      EvaluationContext context) throws IOException {

        QueryResponse response = dataSourceApi.groupByV3(QueryRequest.builder()
                                                                     .dataSource(dataSource)
                                                                     .interval(IntervalRequest.builder()
                                                                                              .startISO8601(start.toISO8601())
                                                                                              .endISO8601(end.toISO8601())
                                                                                              .build())
                                                                     .filterExpression(filterExpression)
                                                                     .fields(Collections.singletonList(metric))
                                                                     .groupBy(groupBy)
                                                                     .build());

        //noinspection unchecked
        List<Map<String, Object>> seriesList = (List<Map<String, Object>>) response.getData();
        if (CollectionUtils.isEmpty(seriesList)) {
            return EvaluationOutputs.EMPTY;
        }

        Map<Label, Number> current = toSeriesMap(seriesList, metric.getName(), groupBy);

        // Find base values for different series
        response = dataSourceApi.groupByV3(QueryRequest.builder()
                                                       .dataSource(dataSource)
                                                       .interval(IntervalRequest.builder()
                                                                                .startISO8601(start.before(this.offset, TimeUnit.SECONDS).toISO8601())
                                                                                .endISO8601(end.before(this.offset, TimeUnit.SECONDS).toISO8601())
                                                                                .build())
                                                       .filterExpression(filterExpression)
                                                       .fields(Collections.singletonList(metric))
                                                       .groupBy(groupBy)
                                                       .build());

        //noinspection unchecked
        Map<Label, Number> baseMap = toSeriesMap((List<Map<String, Object>>) response.getData(), metric.getName(), groupBy);
        if (baseMap.isEmpty()) {
            return EvaluationOutputs.EMPTY;
        }

        EvaluationOutputs outputs = new EvaluationOutputs();

        for (Map.Entry<Label, Number> series : current.entrySet()) {
            Label label = series.getKey();
            Number curr = series.getValue();

            Number val = baseMap.get(series.getKey());
            if (val == null) {
                // No data in given time window, treat it as zero
                val = 0;
            }

            BigDecimal currWindowValue = NumberUtils.scaleTo(curr.doubleValue(), 2);
            BigDecimal baseValue = NumberUtils.scaleTo(val.doubleValue(), 2);
            double delta;
            if (threshold instanceof HumanReadablePercentage) {
                delta = ZERO.equals(baseValue)
                        ? currWindowValue.subtract(baseValue).doubleValue()
                        : currWindowValue.subtract(baseValue).divide(baseValue, 4, RoundingMode.HALF_UP).doubleValue();
            } else {
                delta = currWindowValue.subtract(baseValue).doubleValue();
            }

            String deltaText = threshold instanceof HumanReadablePercentage ? (delta * 100) + "%" : String.valueOf(delta);

            outputs.add(EvaluationOutput.builder()
                                        .matched(matches(delta, threshold.doubleValue()))
                                        .label(label)
                                        .current(currWindowValue.toString())
                                        .threshold(threshold.toString())
                                        .delta(deltaText)
                                        .base(baseValue.toString())
                                        .build());
        }

        return outputs;
    }

    protected abstract boolean matches(double delta, double threshold);

    private Map<Label, Number> toSeriesMap(List<Map<String, Object>> seriesList, String metric, Set<String> groupBy) {
        if (seriesList.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Label, Number> map = new HashMap<>();

        for (Map<String, Object> series : seriesList) {
            Number currValue = (Number) series.get(metric);
            if (currValue == null) {
                continue;
            }

            Label.Builder labelBuilder = Label.builder();
            for (String labelName : groupBy) {
                String labelValue = (String) series.get(labelName);
                labelBuilder.add(labelName, labelValue);
            }
            map.put(labelBuilder.build(), currValue);
        }

        return map;
    }
}
