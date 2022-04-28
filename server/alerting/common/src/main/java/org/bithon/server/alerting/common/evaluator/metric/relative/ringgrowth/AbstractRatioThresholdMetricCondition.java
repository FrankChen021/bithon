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

package org.bithon.server.alerting.common.evaluator.metric.relative.ringgrowth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluatorContext;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.alerting.common.evaluator.result.RatioEvaluationOutput;
import org.bithon.server.alerting.common.model.Aggregator;
import org.bithon.server.alerting.common.model.IMetricCondition;
import org.bithon.server.alerting.common.model.IMetricConditionVisitor;
import org.bithon.server.alerting.common.model.MetricConditionCategory;
import org.bithon.server.alerting.common.utils.NumberUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.web.service.api.GroupByQueryRequest;
import org.bithon.server.web.service.api.IDataSourceApi;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author frankchen
 * @date 2020-08-21 17:06:34
 */
public class AbstractRatioThresholdMetricCondition implements IMetricCondition {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Getter
    @NotNull
    private final Integer minute;

    @Getter
    @NotNull
    private final String name;

    @Getter
    @NotNull
    @Min(1)
    private final Integer percentage;

    @Getter
    private final boolean isUp;

    @Getter
    private final int window;

    @Getter
    private final Aggregator aggregator;

    /**
     * runtime property
     */
    @Getter
    @JsonIgnore
    private final String text;

    public AbstractRatioThresholdMetricCondition(@NotNull String name,
                                                 @NotNull Aggregator aggregator,
                                                 @NotNull Integer minute,
                                                 @Nullable Boolean isUp,
                                                 @NotNull Integer percentage,
                                                 @Nullable Integer window) {
        this.name = name;
        this.aggregator = aggregator;
        this.minute = minute;
        this.percentage = percentage;
        this.isUp = isUp == null || isUp;
        this.window = window;
        this.text = StringUtils.format("[%s]环比%d分钟前%s%d%%",
                                       name,
                                       minute,
                                       this.isUp ? "上涨" : "下跌",
                                       percentage);
    }

    @JsonProperty("category")
    @Override
    public MetricConditionCategory getCategory() {
        return MetricConditionCategory.RELATIVE;
    }

    @Override
    public IEvaluationOutput evaluate(IDataSourceApi dataSourceApi,
                                      String dataSource,
                                      TimeSpan start,
                                      TimeSpan end,
                                      List<IFilter> dimensions,
                                      EvaluatorContext context) {

        List<Map<String, Object>> currWindow = dataSourceApi.groupBy(GroupByQueryRequest.builder()
                                                                                        .dataSource(dataSource)
                                                                                        .startTimeISO8601(start.toISO8601())
                                                                                        .endTimeISO8601(end.toISO8601())
                                                                                        .filters(dimensions)
                                                                                        .metrics(Collections.singletonList(this.name))
                                                                                        .build());

        if (CollectionUtils.isEmpty(currWindow) || !currWindow.get(0).containsKey(this.name)) {
            return null;
        }
        BigDecimal currWindowValue = NumberUtils.scaleTo(((Number) currWindow.get(0).get(this.name)).doubleValue(), 2);

        List<Map<String, Object>> base = dataSourceApi.groupBy(GroupByQueryRequest.builder()
                                                                                  .dataSource(dataSource)
                                                                                  .startTimeISO8601(start.before(this.minute, TimeUnit.MINUTES).toISO8601())
                                                                                  .endTimeISO8601(end.before(this.minute, TimeUnit.MINUTES).toISO8601())
                                                                                  .filters(dimensions)
                                                                                  .metrics(Collections.singletonList(this.name))
                                                                                  .build());

        if (CollectionUtils.isEmpty(base) || !base.get(0).containsKey(this.name)) {
            return null;
        }
        BigDecimal baseValue = NumberUtils.scaleTo(((Number) base.get(0).get(this.name)).doubleValue(), 2);

        double delta;
        if (isUp) {
            delta = ZERO.equals(baseValue)
                    ? currWindowValue.subtract(baseValue).doubleValue()
                    : currWindowValue.subtract(baseValue).divide(baseValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
        } else {
            delta = ZERO.equals(baseValue)
                    ? currWindowValue.subtract(baseValue).doubleValue()
                    : baseValue.subtract(currWindowValue).divide(baseValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        RatioEvaluationOutput output = new RatioEvaluationOutput();
        output.setDelta(delta);
        output.setNow(currWindowValue);
        output.setBase(baseValue);
        output.setThreshold(percentage);
        output.setMatches(delta > percentage);
        output.setConditionId(context.getEvaluatingCondition().getId());
        output.setMetric(this);
        return output;
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public <T> T accept(IMetricConditionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
