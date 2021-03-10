package com.sbss.bithon.server.metric.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sbss.bithon.server.common.matcher.IStringMatcher;
import lombok.Getter;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 4:43 下午
 */
public class DimensionCondition {

    @NotNull
    @Getter
    private final String dimension;

    @NotNull
    @Getter
    private final IStringMatcher matcher;

    @JsonCreator
    public DimensionCondition(@JsonProperty("dimension") String dimension,
                              @JsonProperty("matcher") IStringMatcher matcher) {
        this.dimension = dimension;
        this.matcher = matcher;
    }
}
