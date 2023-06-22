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

package org.bithon.server.storage.datasource.spec.gauge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bithon.server.storage.datasource.aggregator.DoubleLastAggregator;
import org.bithon.server.storage.datasource.aggregator.NumberAggregator;
import org.bithon.server.storage.datasource.typing.IDataType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/23
 */
public class DoubleGaugeMetricSpec extends GaugeMetricSpec {

    @JsonCreator
    public DoubleGaugeMetricSpec(@JsonProperty("name") @NotNull String name,
                                 @JsonProperty("alias") @Nullable String alias,
                                 @JsonProperty("field") @Nullable String field,
                                 @JsonProperty("displayText") @NotNull String displayText) {
        super(name, alias, field, displayText);
    }

    @JsonIgnore
    @Override
    public String getType() {
        return DOUBLE_LAST;
    }

    @Override
    public IDataType getDataType() {
        return IDataType.DOUBLE;
    }

    @Override
    public NumberAggregator createAggregator() {
        return new DoubleLastAggregator();
    }
}
