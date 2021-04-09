package com.sbss.bithon.server.metric.aggregator.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.aggregator.NumberAggregator;
import com.sbss.bithon.server.metric.typing.IValueType;

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
