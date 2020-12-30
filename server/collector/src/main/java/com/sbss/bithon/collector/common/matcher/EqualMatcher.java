package com.sbss.bithon.collector.common.matcher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 4:44 下午
 */
public class EqualMatcher implements IStringMatcher {
    @Getter
    private final String pattern;

    @JsonCreator
    public EqualMatcher(@JsonProperty("pattern") String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(String input) {
        return pattern.equals(input);
    }

    @Override
    public <T> T accept(IMatcherVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
