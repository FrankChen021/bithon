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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.storage.datasource.IColumnSpec;
import org.bithon.server.storage.datasource.aggregator.NumberAggregator;
import org.bithon.server.storage.datasource.query.ast.ResultColumn;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpression;
import org.bithon.server.storage.datasource.spec.gauge.DoubleGaugeMetricSpec;
import org.bithon.server.storage.datasource.spec.gauge.LongGaugeMetricSpec;
import org.bithon.server.storage.datasource.spec.max.LongMaxMetricSpec;
import org.bithon.server.storage.datasource.spec.min.LongMinMetricSpec;
import org.bithon.server.storage.datasource.spec.sum.DoubleSumMetricSpec;
import org.bithon.server.storage.datasource.spec.sum.LongSumMetricSpec;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/11/30 5:36 下午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = IMetricSpec.LONG_SUM, value = LongSumMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.LONG_LAST, value = LongGaugeMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.LONG_MIN, value = LongMinMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.LONG_MAX, value = LongMaxMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.DOUBLE_SUM, value = DoubleSumMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.DOUBLE_LAST, value = DoubleGaugeMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.POST, value = PostAggregatorMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.COUNT, value = CountMetricSpec.class),
})
public interface IMetricSpec extends IColumnSpec {

    /**
     * for Gauge
     */
    String LONG_LAST = "longLast";
    String DOUBLE_LAST = "doubleLast";

    /**
     * for Counter
     */
    String LONG_SUM = "longSum";
    String DOUBLE_SUM = "doubleSum";
    String POST = "post";
    String COUNT = "count";
    String LONG_MIN = "longMin";
    String LONG_MAX = "longMax";

    String getType();

    /**
     * the name in the original message.
     * can be null. if it's null, the {@link #getName()} is used
     */
    String getField();

    <T> T accept(IMetricSpecVisitor<T> visitor);

    NumberAggregator createAggregator();

    SimpleAggregateExpression getAggregateExpression();

    @JsonIgnore
    default ResultColumn getResultColumn() {
        return new ResultColumn(getAggregateExpression(), getName());
    }
}
