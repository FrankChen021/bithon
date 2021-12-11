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
import org.bithon.server.metric.aggregator.NumberAggregator;
import org.bithon.server.metric.typing.IValueType;
import org.bithon.server.metric.typing.LongValueType;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/23
 */
public class CountMetricSpec implements IMetricSpec {

    public static final IMetricSpec INSTANCE = new CountMetricSpec("count");

    @Getter
    private final String name;

    @JsonCreator
    public CountMetricSpec(@JsonProperty("name") @NotNull String name) {
        this.name = name;
    }

    @JsonIgnore
    @Override
    public String getType() {
        return IMetricSpec.LONG_SUM;
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public String getDisplayText() {
        return "次数";
    }

    @Override
    public String getUnit() {
        return "次";
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
        return new NumberAggregator() {
            @Override
            public int intValue() {
                return (int) value;
            }

            @Override
            public long longValue() {
                return value;
            }

            @Override
            public float floatValue() {
                return value;
            }

            @Override
            public double doubleValue() {
                return value;
            }

            private long value;

            @Override
            public void aggregate(long timestamp, Object value) {
                this.value++;
            }
        };
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CountMetricSpec) {
            return this.name.equals(((CountMetricSpec) obj).name);
        } else {
            return false;
        }
    }
}
