package com.sbss.bithon.server.metric.dimension.transformer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author
 * @date
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "simple", value = MappingTransformer.class)
})
public interface IDimensionTransformer {

    Object transform(Object columnValue);
}
