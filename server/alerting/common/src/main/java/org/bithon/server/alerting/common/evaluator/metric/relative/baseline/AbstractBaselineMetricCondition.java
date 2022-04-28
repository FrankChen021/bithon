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

package org.bithon.server.alerting.common.evaluator.metric.relative.baseline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
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
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author frankchen
 * @date 2020-08-21 17:13:21
 */
public class AbstractBaselineMetricCondition implements IMetricCondition {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    @Getter
    private final Integer dayOffset;

    @Getter
    private final String name;

    @Getter
    private final Integer percentage;

    @Getter
    private final boolean isPositive;

    @Getter
    private final int window;

    @Getter
    private final Aggregator aggregator;

    /**
     * runtime property
     */
    @JsonIgnore
    private final String text;

    @JsonIgnore
    private final BaselineMetricCacheManager baseLineCacheManager;

    public AbstractBaselineMetricCondition(@NotNull String name,
                                           @NotNull Aggregator aggregator,
                                           @NotNull Integer dayOffset,
                                           @NotNull Boolean isUp,
                                           @NotNull Integer percentage,
                                           @Nullable Integer window,
                                           BaselineMetricCacheManager baseLineCacheManager) {
        this.name = name;
        this.aggregator = aggregator;
        this.dayOffset = dayOffset;
        this.percentage = percentage;
        this.isPositive = isUp == null || isUp;
        this.window = window == null ? 1 : window;
        this.text = StringUtils.format("[%s]同比%d天前%s%d%%", name, dayOffset, this.isPositive ? "上涨" : "下跌", percentage);
        this.baseLineCacheManager = baseLineCacheManager;

        Preconditions.checkIf(this.window >= 1, "window should be >= 1");
        Preconditions.checkIf(this.window <= 60, "window should be >= 60");
    }

    @Override
    public MetricConditionCategory getCategory() {
        return MetricConditionCategory.RELATIVE;
    }

    @Override
    public Aggregator getAggregator() {
        return null;
    }

    @Override
    public IEvaluationOutput evaluate(IDataSourceApi dataSourceApi,
                                      String dataSource,
                                      TimeSpan start,
                                      TimeSpan end,
                                      List<IFilter> dimensions,
                                      EvaluatorContext context) {

        List<Map<String, Object>> result = dataSourceApi.groupBy(GroupByQueryRequest.builder()
                                                                                    .dataSource(dataSource)
                                                                                    .startTimeISO8601(start.toISO8601())
                                                                                    .endTimeISO8601(end.toISO8601())
                                                                                    .filters(dimensions)
                                                                                    .metrics(Collections.singletonList(this.name))
                                                                                    .build());
        if (CollectionUtils.isEmpty(result)) {
            return null;
        }
        Number value = (Number) result.get(0).get(this.name);
        if (value == null) {
            return null;
        }
        BigDecimal nowValue = NumberUtils.scaleTo(value.doubleValue(), 2);

        TimeSpan baseEnd = context.getIntervalEnd().before(dayOffset, TimeUnit.DAYS);
        TimeSpan baseDayStart = baseEnd.truncate2Day().offset(TimeZone.getDefault());
        TimeSpan baseDayEnd = baseDayStart.after(1, TimeUnit.DAYS);

        context.log(AbstractBaselineMetricCondition.class, "Loading metric from baseline in [%s, %s]", baseDayStart.toISO8601(), baseDayEnd.toISO8601());
        List<Map<String, Object>> baseline = baseLineCacheManager.getBaselineMetricsList(baseDayStart,
                                                                                         baseDayEnd,
                                                                                         dataSource,
                                                                                         dimensions,
                                                                                         this.name);

        String timePoint = DateTime.formatDateTime("HH:mm", start.getMilliseconds());
        Optional<Map<String, Object>> baseValueObject = baseline.stream()
                                                                .filter(base -> timePoint.equals(base.get("_time")))
                                                                .findFirst();
        if (!baseValueObject.isPresent()) {
            context.log(AbstractBaselineMetricCondition.class, "基准时间点[%s]数据不存在", timePoint);
            return null;
        }

        Number val = (Number) baseValueObject.get().get(this.name);
        if (val == null) {
            return null;
        }
        BigDecimal base = NumberUtils.scaleTo(val.doubleValue(), 2);

        double delta;
        if (isPositive) {
            delta = ZERO.equals(base)
                    ? nowValue.subtract(base).doubleValue()
                    : nowValue.subtract(base).divide(base, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
        } else {
            delta = ZERO.equals(base)
                    ? nowValue.subtract(base).doubleValue()
                    : base.subtract(nowValue).divide(base, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        RatioEvaluationOutput output = new RatioEvaluationOutput();
        output.setDelta(delta);
        output.setNow(nowValue);
        output.setBase(base);
        output.setThreshold(this.percentage);
        output.setMatches(delta > this.percentage);
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
