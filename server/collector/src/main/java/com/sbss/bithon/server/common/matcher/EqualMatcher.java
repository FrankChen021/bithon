package com.sbss.bithon.server.common.matcher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 4:44 下午
 */
public class EqualMatcher implements IStringMatcher {
    @Getter
    @NotNull
    private final String pattern;

    @JsonCreator
    public EqualMatcher(@JsonProperty("pattern") @NotNull String pattern) {
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
