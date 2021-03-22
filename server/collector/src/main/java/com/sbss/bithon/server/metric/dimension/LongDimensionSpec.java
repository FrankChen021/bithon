package com.sbss.bithon.server.metric.dimension;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sbss.bithon.server.metric.dimension.transformer.IDimensionTransformer;
import com.sbss.bithon.server.metric.typing.IValueType;
import com.sbss.bithon.server.metric.typing.LongValueType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 10:16 上午
 */
public class LongDimensionSpec extends AbstractDimensionSpec {

    @JsonCreator
    public LongDimensionSpec(@JsonProperty("name") @NotNull String name,
                             @JsonProperty("displayText") @NotNull String displayText,
                             @JsonProperty("required") @Nullable Boolean required,
                             @JsonProperty("visible") @Nullable Boolean visible,
                             @JsonProperty("transformer") @Nullable IDimensionTransformer transformer) {
        super(name, displayText, required, visible, transformer);
    }

    @Override
    public IValueType getValueType() {
        return LongValueType.INSTANCE;
    }

    @Override
    public <T> T accept(IDimensionSpecVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
