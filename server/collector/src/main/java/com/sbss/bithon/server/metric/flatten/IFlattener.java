package com.sbss.bithon.server.metric.flatten;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/28
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
        //@JsonSubTypes.Type(name = "path", value = JSONPathFlattener.class),
})
public interface IFlattener {

    String getName();
    Object flatten(JsonNode jsonNode);
}
