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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.query.IQueryStageAggregatorVisitor;


/**
 * Aggregator for built-in types.
 *
 * @author Frank Chen
 * @date 1/11/21 2:36 pm
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = SimpleAggregators.CardinalityAggregator.TYPE, value = SimpleAggregators.CardinalityAggregator.class),
    @JsonSubTypes.Type(name = SimpleAggregators.SumAggregator.TYPE, value = SimpleAggregators.SumAggregator.class),
    @JsonSubTypes.Type(name = SimpleAggregators.CountAggregator.TYPE, value = SimpleAggregators.CountAggregator.class),
    @JsonSubTypes.Type(name = SimpleAggregators.AvgAggregator.TYPE, value = SimpleAggregators.AvgAggregator.class),
    @JsonSubTypes.Type(name = SimpleAggregators.MinAggregator.TYPE, value = SimpleAggregators.MinAggregator.class),
    @JsonSubTypes.Type(name = SimpleAggregators.MaxAggregator.TYPE, value = SimpleAggregators.MaxAggregator.class),
    @JsonSubTypes.Type(name = SimpleAggregators.FirstAggregator.TYPE, value = SimpleAggregators.FirstAggregator.class),
    @JsonSubTypes.Type(name = SimpleAggregators.LastAggregator.TYPE, value = SimpleAggregators.LastAggregator.class),
    @JsonSubTypes.Type(name = SimpleAggregators.RateAggregator.TYPE, value = SimpleAggregators.RateAggregator.class),
    @JsonSubTypes.Type(name = SimpleAggregators.GroupConcatAggregator.TYPE, value = SimpleAggregators.GroupConcatAggregator.class),
})
public abstract class SimpleAggregator extends Function {
    public SimpleAggregator(String fnName) {
        super(fnName);
    }

    @JsonIgnore
    public String getType() {
        return this.getFnName();
    }

    /**
     * target field, default to name
     */
    public String getTargetField() {
        return ((Name)this.getArguments().get(0)).getName();
    }

    public abstract <T> T accept(IQueryStageAggregatorVisitor<T> visitor);

    @Override
    public String toString() {
        return StringUtils.format("%s(%s)", this.getFnName(), getTargetField());
    }
}
