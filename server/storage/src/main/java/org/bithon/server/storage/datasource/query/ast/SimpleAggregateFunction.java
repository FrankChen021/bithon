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

package org.bithon.server.storage.datasource.query.ast;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.component.commons.utils.StringUtils;


/**
 * Aggregator for built-in types.
 *
 * @author Frank Chen
 * @date 1/11/21 2:36 pm
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = SimpleAggregateFunctions.CardinalityAggregateFunction.TYPE, value = SimpleAggregateFunctions.CardinalityAggregateFunction.class),
    @JsonSubTypes.Type(name = SimpleAggregateFunctions.SumAggregateFunction.TYPE, value = SimpleAggregateFunctions.SumAggregateFunction.class),
    @JsonSubTypes.Type(name = SimpleAggregateFunctions.CountAggregateFunction.TYPE, value = SimpleAggregateFunctions.CountAggregateFunction.class),
    @JsonSubTypes.Type(name = SimpleAggregateFunctions.AvgAggregateFunction.TYPE, value = SimpleAggregateFunctions.AvgAggregateFunction.class),
    @JsonSubTypes.Type(name = SimpleAggregateFunctions.MinAggregateFunction.TYPE, value = SimpleAggregateFunctions.MinAggregateFunction.class),
    @JsonSubTypes.Type(name = SimpleAggregateFunctions.MaxAggregateFunction.TYPE, value = SimpleAggregateFunctions.MaxAggregateFunction.class),
    @JsonSubTypes.Type(name = SimpleAggregateFunctions.FirstAggregateFunction.TYPE, value = SimpleAggregateFunctions.FirstAggregateFunction.class),
    @JsonSubTypes.Type(name = SimpleAggregateFunctions.LastAggregateFunction.TYPE, value = SimpleAggregateFunctions.LastAggregateFunction.class),
    @JsonSubTypes.Type(name = SimpleAggregateFunctions.RateAggregateFunction.TYPE, value = SimpleAggregateFunctions.RateAggregateFunction.class),
    @JsonSubTypes.Type(name = SimpleAggregateFunctions.GroupConcatAggregateFunction.TYPE, value = SimpleAggregateFunctions.GroupConcatAggregateFunction.class),
})
public abstract class SimpleAggregateFunction extends Function {
    public SimpleAggregateFunction(String fnName, String field) {
        super(fnName);
        getArguments().add(new Column(field));
    }

    /**
     * get the column that aggregation is performed on
     */
    public String getTargetColumn() {
        return ((Column) this.getArguments().get(0)).getName();
    }

    public abstract <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor);

    @Override
    public String toString() {
        return StringUtils.format("%s(%s)", this.getFnName(), getTargetColumn());
    }
}
