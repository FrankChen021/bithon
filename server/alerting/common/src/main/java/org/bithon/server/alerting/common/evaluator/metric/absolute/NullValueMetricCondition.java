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

import com.fasterxml.jackson.annotation.JsonCreator;
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
import org.bithon.server.alerting.common.model.IMetricConditionVisitor;
import org.bithon.server.alerting.common.model.MetricConditionCategory;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.typing.IValueType;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.web.service.api.GroupByQueryRequest;
import org.bithon.server.web.service.api.IDataSourceApi;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 28/3/22 10:53 PM
 */
public class NullValueMetricCondition implements IMetricCondition {

    @Getter
    @NotNull
    private final String name;

    @Getter
    private final int window;

    /**
     * runtime property
     */
    @Getter
    @JsonIgnore
    private final String text;

    @JsonCreator
    public NullValueMetricCondition(@JsonProperty("name") @NotNull String name,
                                    @JsonProperty("window") @Nullable Integer window) {

        this.name = Preconditions.checkArgumentNotNull("name", name);
        this.text = StringUtils.format("%s is null", name);
        this.window = window == null ? 1 : window;
        Preconditions.checkIf(this.window >= 1, "window should be >= 1");
        Preconditions.checkIf(this.window <= 60, "window should be <= 60");
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
                                                                                 .metrics(Collections.singletonList(this.name))
                                                                                 .build());
        boolean matches = false;
        Number nowValue = null;
        if (CollectionUtils.isEmpty(now) || !now.get(0).containsKey(this.name)) {
            matches = true;
        } else {
            nowValue = (Number) now.get(0).get(this.name);
        }

        IValueType valueType = dataSourceApi.getSchemaByName(dataSource).getMetricSpecByName(this.name).getValueType();
        return new ValueEvaluationOutput(context.getEvaluatingCondition().getId(),
                                         context.getEvaluatingMetric(),
                                         nowValue == null ? null : valueType.format(nowValue),
                                         "null",
                                         nowValue == null ? null : nowValue.toString(),
                                         matches);
    }

    @JsonProperty("category")
    @Override
    public MetricConditionCategory getCategory() {
        return MetricConditionCategory.ABSOLUTE;
    }

    @Override
    public Aggregator getAggregator() {
        return null;
    }

    @Override
    public <T> T accept(IMetricConditionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return text;
    }
}
