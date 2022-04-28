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

package org.bithon.server.alerting.common.evaluator.metric.absolute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluatorContext;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.alerting.common.evaluator.result.ValueEvaluationOutput;
import org.bithon.server.alerting.common.model.Aggregator;
import org.bithon.server.alerting.common.model.IMetricCondition;
import org.bithon.server.alerting.common.model.MetricConditionCategory;
import org.bithon.server.alerting.common.utils.HumanReadableBytes;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.api.IQueryStageAggregator;
import org.bithon.server.storage.datasource.typing.IValueType;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.web.service.api.GroupByQueryRequest;
import org.bithon.server.web.service.api.IDataSourceApi;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/14 4:22 下午
 */
public abstract class AbstractAbsoluteThresholdCondition implements IMetricCondition {

    @Getter
    @NotNull
    private final String name;

    @Getter
    @NotNull
    private final Object expected;

    @Getter
    private final int window;

    /**
     * runtime property
     */
    @JsonIgnore
    private final Number expectedValue;

    @Getter
    private final Aggregator aggregator;

    /**
     * runtime property
     */
    @Getter
    @JsonIgnore
    private final String stringText;

    @JsonIgnore
    private final List<IQueryStageAggregator> queryStageAggregators;

    public AbstractAbsoluteThresholdCondition(String name,
                                              Aggregator aggregator,
                                              Object expected,
                                              String operator,
                                              Integer window) {

        this.name = Preconditions.checkArgumentNotNull("name", name);
        this.aggregator = Preconditions.checkArgumentNotNull("aggregator", aggregator);
        this.queryStageAggregators = Collections.singletonList(this.aggregator.create(name));

        this.expected = Preconditions.checkArgumentNotNull("expected", expected);
        this.window = window == null ? 1 : window;
        Preconditions.checkIf(this.window >= 1, "window should be >= 1");
        Preconditions.checkIf(this.window <= 60, "window should be <= 60");

        Number tmp;
        if (expected instanceof Number) {
            tmp = (Number) expected;
        } else if (expected instanceof String) {
            try {
                tmp = HumanReadableBytes.parse((String) expected);
            } catch (HumanReadableBytes.IAE e) {
                tmp = Double.parseDouble((String) expected);
            }
        } else {
            throw new IllegalArgumentException("invalid argument [expected]: " + expected);
        }
        this.expectedValue = tmp;

        this.stringText = StringUtils.format("%s(%s, -%dm) %s %s",
                                             this.aggregator,
                                             name,
                                             this.window,
                                             Preconditions.checkArgumentNotNull("operator", operator),
                                             expected);
    }

    @Override
    public IEvaluationOutput evaluate(IDataSourceApi dataSourceApi,
                                      String dataSource,
                                      TimeSpan start,
                                      TimeSpan end,
                                      List<IFilter> dimensions,
                                      EvaluatorContext context) {
        List<Map<String, Object>> now = dataSourceApi.groupBy(GroupByQueryRequest.builder()
                                                                                 .dataSource(dataSource)
                                                                                 .startTimeISO8601(start.toISO8601())
                                                                                 .endTimeISO8601(end.toISO8601())
                                                                                 .filters(dimensions)
                                                                                 .aggregators(queryStageAggregators)
                                                                                 .build());
        if (CollectionUtils.isEmpty(now) || !now.get(0).containsKey(this.name)) {
            return null;
        }
        Number nowValue = (Number) now.get(0).get(this.name);
        if (nowValue == null) {
            return null;
        }

        IValueType valueType = dataSourceApi.getSchemaByName(dataSource).getMetricSpecByName(this.name).getValueType();
        return new ValueEvaluationOutput(context.getEvaluatingCondition().getId(),
                                         context.getEvaluatingMetric(),
                                         valueType.format(nowValue),
                                         expected.toString(),
                                         valueType.format(valueType.diff(nowValue, expectedValue)),
                                         matches(valueType, expectedValue, nowValue));
    }

    @JsonProperty("category")
    @Override
    public MetricConditionCategory getCategory() {
        return MetricConditionCategory.ABSOLUTE;
    }

    @Override
    public String toString() {
        return stringText;
    }

    protected abstract boolean matches(IValueType valueType, Number threshold, Number now);
}
