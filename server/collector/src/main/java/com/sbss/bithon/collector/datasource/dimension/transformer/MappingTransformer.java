package com.sbss.bithon.collector.datasource.dimension.transformer;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * @author
 * @date
 */
public class MappingTransformer implements IDimensionTransformer {

    private final Map<String, Object> maps;

    public MappingTransformer(@JsonProperty("maps") @NotNull Map<String, Object> maps) {
        this.maps = maps;
    }

    @Override
    public Object transform(Object dimensionValue) {
        if (dimensionValue == null) {
            return null;
        }
        return maps.getOrDefault((String) dimensionValue, dimensionValue);
    }
}
