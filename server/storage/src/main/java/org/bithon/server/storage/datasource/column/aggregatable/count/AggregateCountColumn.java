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

package org.bithon.server.storage.datasource.column.aggregatable.count;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.server.storage.datasource.aggregator.NumberAggregator;
import org.bithon.server.storage.datasource.column.aggregatable.IAggregatableColumn;
import org.bithon.server.storage.datasource.query.ast.QueryAggregateFunction;
import org.bithon.server.storage.datasource.query.ast.QueryAggregateFunctions;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/23
 */
public class AggregateCountColumn implements IAggregatableColumn {

    public static final IAggregatableColumn INSTANCE = new AggregateCountColumn("count", "count");

    @Getter
    private final String name;

    private final QueryAggregateFunction queryStageAggregator;

    @Getter
    private final String alias;

    @JsonCreator
    public AggregateCountColumn(@JsonProperty("name") @NotNull String name,
                                @JsonProperty("alias") @Nullable String alias) {
        this.name = name;
        this.alias = alias == null ? name : alias;
        this.queryStageAggregator = new QueryAggregateFunctions.CountAggregateExpression(name);
    }

    @Override
    public IDataType getDataType() {
        return IDataType.LONG;
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
    public QueryAggregateFunction getAggregateExpression() {
        return queryStageAggregator;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AggregateCountColumn) {
            return this.name.equals(((AggregateCountColumn) obj).name);
        } else {
            return false;
        }
    }
}
