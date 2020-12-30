package com.sbss.bithon.collector.common.matcher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/13 9:50 下午
 */
public class AntPathMatcher implements IStringMatcher {

    @Getter
    private final String pattern;

    @JsonCreator
    public AntPathMatcher(@JsonProperty("pattern") String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(String input) {
        return new org.springframework.util.AntPathMatcher().match(pattern, input);
    }

    @Override
    public <T> T accept(IMatcherVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
