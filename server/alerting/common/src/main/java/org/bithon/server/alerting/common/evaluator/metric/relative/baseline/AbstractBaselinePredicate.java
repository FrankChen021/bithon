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
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.NumberUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.alerting.common.evaluator.result.RelativeComparisonEvaluationOutput;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryField;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author frankchen
 * @date 2020-08-21 17:13:21
 */
public class AbstractBaselinePredicate implements IMetricEvaluator {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    @Getter
    private final int dayBefore;

    @Getter
    private final int percentage;

    @Getter
    private final boolean isPositive;


    @JsonIgnore
    private final BaselineMetricCacheManager baseLineCacheManager;

    public AbstractBaselinePredicate(@NotNull Integer dayBefore,
                                     boolean isUp,
                                     @NotNull Integer percentage,
                                     BaselineMetricCacheManager baseLineCacheManager) {
        this.dayBefore = Preconditions.checkArgumentNotNull("dayOffset", dayBefore);
        this.percentage = Preconditions.checkArgumentNotNull("percentage", percentage);
        this.isPositive = isUp;
        this.baseLineCacheManager = baseLineCacheManager;

        Preconditions.checkIfTrue(percentage > 0 && percentage <= 100, "percentage must be in the range of (0,100].");
        Preconditions.checkIfTrue(this.dayBefore >= 1, "dayOffset must be >= 1");
    }

    @Override
    public IEvaluationOutput evaluate(IDataSourceApi dataSourceApi,
                                      String dataSource,
                                      QueryField metric,
                                      TimeSpan start,
                                      TimeSpan end,
                                      String filterExpression,
                                      Set<String> groupBy,
                                      EvaluationContext context) throws IOException {

        QueryResponse response = dataSourceApi.groupBy(QueryRequest.builder()
                                                                   .dataSource(dataSource)
                                                                   .interval(IntervalRequest.builder()
                                                                                            .startISO8601(start.toISO8601())
                                                                                            .endISO8601(end.toISO8601())
                                                                                            .build())
                                                                   .filterExpression(filterExpression)
                                                                   .fields(Collections.singletonList(metric))
                                                                   .groupBy(groupBy)
                                                                   .build());
        if (CollectionUtils.isEmpty(response.getData())) {
            return null;
        }

        //noinspection unchecked
        List<Map<String, Object>> result = (List<Map<String, Object>>) response.getData();
        Number value = (Number) result.get(0).get(metric.getName());
        if (value == null) {
            return null;
        }
        BigDecimal nowValue = NumberUtils.scaleTo(value.doubleValue(), 2);

        TimeSpan baseEnd = context.getIntervalEnd().before(dayBefore, TimeUnit.DAYS);
        TimeSpan baseDayStart = baseEnd.floor(Duration.ofDays(1)).offset(TimeZone.getDefault());
        TimeSpan baseDayEnd = baseDayStart.after(1, TimeUnit.DAYS);

        int window = (int) (end.getMilliseconds() - start.getMilliseconds()) / 1000;
        context.log(AbstractBaselinePredicate.class, "Loading metric from baseline in [%s, %s]", baseDayStart.toISO8601(), baseDayEnd.toISO8601());
        List<Number> baseline = baseLineCacheManager.getBaselineMetricsList(baseDayStart,
                                                                            baseDayEnd,
                                                                            window,
                                                                            dataSource,
                                                                            filterExpression,
                                                                            metric);

        // get index of current data
        long millis = start.minus(Duration.ofDays(1).toMillis()).diff(baseDayStart);
        long index = millis / 1000 / 60;

        Number val = baseline.get((int) index);
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

        RelativeComparisonEvaluationOutput output = new RelativeComparisonEvaluationOutput();
        output.setDelta(delta);
        output.setNow(nowValue);
        output.setBase(base);
        output.setThreshold(this.percentage);
        output.setMatches(delta > this.percentage);
        output.setMetric(this);
        output.setStart(start);
        output.setEnd(end);
        return output;
    }
}
