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
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bithon.server.alerting.common.model.AggregatorEnum;
import org.bithon.server.alerting.common.model.IMetricConditionVisitor;
import org.bithon.server.storage.datasource.typing.IValueType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/7 6:55 下午
 */
public class GreaterThanMetricCondition extends AbstractAbsoluteThresholdCondition {

    @JsonCreator
    public GreaterThanMetricCondition(@JsonProperty("name") @NotNull String name,
                                      @JsonProperty("aggregator") @NotNull AggregatorEnum aggregator,
                                      @JsonProperty("expected") @NotNull Object expected,
                                      @JsonProperty("window") @Nullable Integer window) {
        super(name, aggregator, expected, ">", window);
    }

    @Override
    protected boolean matches(IValueType valueType, Number threshold, Number now) {
        return valueType.isGreaterThan(now, threshold);
    }

    @Override
    public <T> T accept(IMetricConditionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
