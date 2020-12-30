package com.sbss.bithon.collector.datasource.filter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sbss.bithon.collector.datasource.input.InputRow;

/**
 * @author frankchen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
        @JsonSubTypes.Type(name = "==", value = EqualFilter.class),
})
public interface IFilter {

    boolean shouldInclude(InputRow inputRow);
}
