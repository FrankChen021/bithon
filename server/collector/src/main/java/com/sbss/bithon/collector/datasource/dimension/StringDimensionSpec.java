package com.sbss.bithon.collector.datasource.dimension;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sbss.bithon.collector.datasource.dimension.transformer.IDimensionTransformer;
import com.sbss.bithon.collector.datasource.typing.IValueType;
import com.sbss.bithon.collector.datasource.typing.StringValueType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 10:11 上午
 */
public class StringDimensionSpec extends AbstractDimensionSpec {

    @JsonCreator
    public StringDimensionSpec(@JsonProperty("field") @Nullable String field,
                               @JsonProperty("name") @NotNull String name,
                               @JsonProperty("displayText") @NotNull String displayText,
                               @JsonProperty("isRequired") Boolean isRequired,
                               @JsonProperty("isVisible") Boolean isVisible,
                               @JsonProperty("transformer") @Nullable IDimensionTransformer transformer) {
        super(field, name, displayText, isRequired, isVisible, transformer);
    }

    @Override
    public IValueType getValueType() {
        return StringValueType.INSTANCE;
    }

    @Override
    public <T> T accept(IDimensionSpecVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
