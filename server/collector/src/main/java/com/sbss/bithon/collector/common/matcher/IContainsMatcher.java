package com.sbss.bithon.collector.common.matcher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/13 9:44 下午
 */
public class IContainsMatcher implements IStringMatcher {

    @Getter
    private final String pattern;

    @JsonCreator
    public IContainsMatcher(@JsonProperty("pattern") String pattern) {
        this.pattern = pattern.toLowerCase();
    }

    @Override
    public boolean matches(String input) {
        return input.toLowerCase().contains(pattern);
    }

    @Override
    public <T> T accept(IMatcherVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
