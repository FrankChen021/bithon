package com.sbss.bithon.server.metric.filter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sbss.bithon.server.metric.input.InputRow;

/**
 * @author frankchen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
        @JsonSubTypes.Type(name = "endwith", value = EndwithFilter.class),
        @JsonSubTypes.Type(name = "==", value = EqualFilter.class),
        @JsonSubTypes.Type(name = ">", value = GreaterThanFilter.class),
        @JsonSubTypes.Type(name = "or", value = OrFilter.class)
})
public interface IFilter {

    boolean shouldInclude(InputRow inputRow);
}
