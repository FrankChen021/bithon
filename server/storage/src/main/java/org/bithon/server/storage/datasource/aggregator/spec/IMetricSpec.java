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

package org.bithon.server.storage.datasource.aggregator.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.aggregator.NumberAggregator;
import org.bithon.server.storage.datasource.typing.IValueType;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/11/30 5:36 下午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = IMetricSpec.LONG_SUM, value = LongSumMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.LONG_LAST, value = LongLastMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.LONG_MIN, value = LongMinMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.LONG_MAX, value = LongMaxMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.DOUBLE_SUM, value = DoubleSumMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.DOUBLE_LAST, value = DoubleLastMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.POST, value = PostAggregatorMetricSpec.class),
})
public interface IMetricSpec {

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

    String getName();

    /**
     * 是否对配置可见
     */
    boolean isVisible();

    /**
     * 前端显示字段名称
     */
    String getDisplayText();

    /**
     * 指标单位
     */
    String getUnit();

    @JsonIgnore
    IValueType getValueType();

    void setOwner(DataSourceSchema dataSource);

    /**
     * 校验输入
     *
     * @return 验证结果，如果非null则为错误提示信息
     */
    String validate(Object input);

    <T> T accept(IMetricSpecVisitor<T> visitor);

    NumberAggregator createAggregator();
}
