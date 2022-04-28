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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bithon.server.alerting.common.model.Aggregator;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * Minute Over Minute Up
 *
 * @author frankchen
 * @date 2020-08-21 17:06:34
 */
public class MoMUpMetricCondition extends AbstractRatioThresholdMetricCondition {

    @JsonCreator
    public MoMUpMetricCondition(@NotNull @JsonProperty("name") String metric,
                                @JsonProperty("aggregator") @NotNull Aggregator aggregator,
                                @NotNull @JsonProperty("minute") Integer minute,
                                @NotNull @JsonProperty("percentage") Integer percentage,
                                @Nullable @JsonProperty("window") Integer window) {
        super(metric, aggregator, minute, true, percentage, window);
    }
}
