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

package org.bithon.server.metric.aggregator.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.aggregator.LongSumAggregator;
import org.bithon.server.metric.aggregator.NumberAggregator;
import org.bithon.server.metric.typing.IValueType;
import org.bithon.server.metric.typing.LongValueType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/11/30 5:38 下午
 */
public class LongSumMetricSpec implements IMetricSpec {

    @Getter
    private final String name;

    @Getter
    private final String displayText;

    @Getter
    private final String unit;

    @Getter
    private final boolean visible;

    @JsonCreator
    public LongSumMetricSpec(@JsonProperty("name") @NotNull String name,
                             @JsonProperty("displayText") @NotNull String displayText,
                             @JsonProperty("unit") @NotNull String unit,
                             @JsonProperty("visible") @Nullable Boolean visible) {
        this.name = name;
        this.displayText = displayText;
        this.unit = unit;
        this.visible = visible == null ? true : visible;
    }

    @JsonIgnore
    @Override
    public String getType() {
        return IMetricSpec.LONG_SUM;
    }

    @Override
    public IValueType getValueType() {
        return LongValueType.INSTANCE;
    }

    @Override
    public void setOwner(DataSourceSchema dataSource) {
    }

    @Override
    public String validate(Object input) {
        return null;
    }

    @Override
    public <T> T accept(IMetricSpecVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public NumberAggregator createAggregator() {
        return new LongSumAggregator();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LongSumMetricSpec) {
            return this.name.equals(((LongSumMetricSpec) obj).name);
        } else {
            return false;
        }
    }
}
