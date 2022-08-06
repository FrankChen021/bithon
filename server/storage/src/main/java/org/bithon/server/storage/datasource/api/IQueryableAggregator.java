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

package org.bithon.server.storage.datasource.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Frank Chen
 * @date 1/11/21 2:36 pm
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = CardinalityAggregator.TYPE, value = CardinalityAggregator.class),
    @JsonSubTypes.Type(name = GroupConcatAggregator.TYPE, value = GroupConcatAggregator.class),
    @JsonSubTypes.Type(name = SimpleAggregator.MinAggregator.TYPE, value = SimpleAggregator.MinAggregator.class),
    @JsonSubTypes.Type(name = SimpleAggregator.MaxAggregator.TYPE, value = SimpleAggregator.MaxAggregator.class),
    @JsonSubTypes.Type(name = SimpleAggregator.SumAggregator.TYPE, value = SimpleAggregator.SumAggregator.class),
    @JsonSubTypes.Type(name = SimpleAggregator.AvgAggregator.TYPE, value = SimpleAggregator.AvgAggregator.class),
    @JsonSubTypes.Type(name = CountAggregator.TYPE, value = CountAggregator.class)
})
public interface IQueryableAggregator {
    <T> T accept(IQueryableAggregatorVisitor<T> visitor);
}
