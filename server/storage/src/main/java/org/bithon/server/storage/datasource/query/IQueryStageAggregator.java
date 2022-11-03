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

package org.bithon.server.storage.datasource.query;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


/**
 * @author Frank Chen
 * @date 1/11/21 2:36 pm
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = QueryStageAggregators.CardinalityAggregator.TYPE, value = QueryStageAggregators.CardinalityAggregator.class),
    @JsonSubTypes.Type(name = QueryStageAggregators.SumAggregator.TYPE, value = QueryStageAggregators.SumAggregator.class),
    @JsonSubTypes.Type(name = QueryStageAggregators.CountAggregator.TYPE, value = QueryStageAggregators.CountAggregator.class),
    @JsonSubTypes.Type(name = QueryStageAggregators.AvgAggregator.TYPE, value = QueryStageAggregators.AvgAggregator.class),
    @JsonSubTypes.Type(name = QueryStageAggregators.MinAggregator.TYPE, value = QueryStageAggregators.MinAggregator.class),
    @JsonSubTypes.Type(name = QueryStageAggregators.MaxAggregator.TYPE, value = QueryStageAggregators.MaxAggregator.class),
    @JsonSubTypes.Type(name = QueryStageAggregators.FirstAggregator.TYPE, value = QueryStageAggregators.FirstAggregator.class),
    @JsonSubTypes.Type(name = QueryStageAggregators.LastAggregator.TYPE, value = QueryStageAggregators.LastAggregator.class),
    @JsonSubTypes.Type(name = QueryStageAggregators.RateAggregator.TYPE, value = QueryStageAggregators.RateAggregator.class),
    @JsonSubTypes.Type(name = QueryStageAggregators.GroupConcatAggregator.TYPE, value = QueryStageAggregators.GroupConcatAggregator.class),
})
public interface IQueryStageAggregator {

    String getType();

    /**
     * target field, default to name
     */
    String getField();

    /**
     * output name
     */
    String getName();

    <T> T accept(IQueryStageAggregatorVisitor<T> visitor);
}
