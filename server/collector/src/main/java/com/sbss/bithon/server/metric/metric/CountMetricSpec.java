package com.sbss.bithon.server.metric.metric;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.typing.IValueType;
import com.sbss.bithon.server.metric.typing.LongValueType;
import lombok.Getter;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/23
 */
public class CountMetricSpec implements IMetricSpec {

    public static IMetricSpec INSTANCE = new CountMetricSpec("count");

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
