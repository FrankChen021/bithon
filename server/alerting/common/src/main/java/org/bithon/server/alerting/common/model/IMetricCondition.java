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

package org.bithon.server.alerting.common.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.alerting.common.evaluator.IConditionEvaluator;
import org.bithon.server.alerting.common.evaluator.metric.absolute.GreaterOrEqualMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.GreaterThanMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.LessOrEqualMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.LessThanMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.NullValueMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.relative.baseline.DoDDownMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.relative.baseline.DoDUpMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.relative.ringgrowth.MoMDownMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.relative.ringgrowth.MoMUpMetricCondition;
import org.bithon.server.storage.datasource.api.IQueryStageAggregator;

/**
 * @author frankchen
 * @date 2020-08-21 16:14:56
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "comparator")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "mom-up", value = MoMUpMetricCondition.class),
    @JsonSubTypes.Type(name = "mom-down", value = MoMDownMetricCondition.class),
    @JsonSubTypes.Type(name = "dod-up", value = DoDUpMetricCondition.class),
    @JsonSubTypes.Type(name = "dod-down", value = DoDDownMetricCondition.class),
    @JsonSubTypes.Type(name = ">", value = GreaterThanMetricCondition.class),
    @JsonSubTypes.Type(name = ">=", value = GreaterOrEqualMetricCondition.class),
    @JsonSubTypes.Type(name = "<=", value = LessOrEqualMetricCondition.class),
    @JsonSubTypes.Type(name = "<", value = LessThanMetricCondition.class),
    @JsonSubTypes.Type(name = "null-value", value = NullValueMetricCondition.class)
})
public interface IMetricCondition extends IConditionEvaluator {

    MetricConditionCategory getCategory();

    String getName();

    AggregatorEnum getAggregator();

    default IQueryStageAggregator createAggregator() {
        return getAggregator().create(getName());
    }

    /**
     * how long the metric should be calculated in the interval [now - duration, now)
     * In minute
     */
    int getWindow();

    <T> T accept(IMetricConditionVisitor<T> visitor);
}
