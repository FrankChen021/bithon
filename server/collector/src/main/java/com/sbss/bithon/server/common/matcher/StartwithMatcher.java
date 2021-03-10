package com.sbss.bithon.server.common.matcher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/13 9:47 下午
 */
public class StartwithMatcher implements IStringMatcher {

    @Getter
    private final String pattern;

    @JsonCreator
    public StartwithMatcher(@JsonProperty("pattern") String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(String input) {
        return input.startsWith(pattern);
    }

    @Override
    public <T> T accept(IMatcherVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
