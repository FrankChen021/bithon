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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import jakarta.validation.constraints.NotNull;

/**
 * @author frankchen
 * @date 2020-08-21 17:13:21
 */
public class DoDDownPredicate extends AbstractBaselinePredicate {

    public DoDDownPredicate(@JsonProperty("dayBefore") @NotNull Integer dayBefore,
                            @JsonProperty("percentage") @NotNull Integer percentage,
                            @JacksonInject(useInput = OptBoolean.FALSE) BaselineMetricCacheManager baseLineCacheManager) {
        super(dayBefore, false, percentage, baseLineCacheManager);
    }
}
