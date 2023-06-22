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

package org.bithon.server.storage.datasource.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.aggregator.NumberAggregator;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpression;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpressions;
import org.bithon.server.storage.datasource.typing.IDataType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/23
 */
public class CountMetricSpec implements IMetricSpec {

    public static final IMetricSpec INSTANCE = new CountMetricSpec("count", "count");

    @Getter
    private final String name;

    @Getter
    private final String field;
    private final SimpleAggregateExpression queryStageAggregator;

    @JsonCreator
    public CountMetricSpec(@JsonProperty("name") @NotNull String name,
                           @JsonProperty("field") @Nullable String field) {
        this.name = name;
        this.field = field;
        this.queryStageAggregator = new SimpleAggregateExpressions.CountAggregateExpression(name);
    }

    @JsonIgnore
    @Override
    public String getType() {
        return IMetricSpec.COUNT;
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
    public IDataType getDataType() {
        return IDataType.LONG;
    }

    @Override
    public void setOwner(DataSourceSchema dataSource) {
    }

    @Override
    public <T> T accept(IMetricSpecVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public NumberAggregator createAggregator() {
        return new NumberAggregator() {
            private long value;

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

            @Override
            public void aggregate(long timestamp, Object value) {
                this.value++;
            }

            @Override
            public Number getNumber() {
                return value;
            }
        };
    }

    @JsonIgnore
    @Override
    public SimpleAggregateExpression getAggregateExpression() {
        return queryStageAggregator;
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
