package com.sbss.bithon.collector.datasource.dimension;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sbss.bithon.collector.datasource.dimension.transformer.IDimensionTransformer;
import com.sbss.bithon.collector.datasource.typing.IValueType;

/**
 * @author frankchen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = StringDimensionSpec.class)
@JsonSubTypes(value = {
        @JsonSubTypes.Type(name = "long", value = LongDimensionSpec.class),
        @JsonSubTypes.Type(name = "string", value = StringDimensionSpec.class)
})
public interface IDimensionSpec {

    String getField();

    String getName();

    String getDisplayText();

    boolean isRequired();

    /**
     * 对用户是否可见
     */
    boolean isVisible();

    IDimensionTransformer getTransformer();

    @JsonIgnore
    IValueType getValueType();

    <T> T accept(IDimensionSpecVisitor<T> visitor);
}