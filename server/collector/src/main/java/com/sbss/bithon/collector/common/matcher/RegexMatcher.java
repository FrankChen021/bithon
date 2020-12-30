package com.sbss.bithon.collector.common.matcher;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sbss.bithon.collector.common.matcher.IStringMatcher;
import lombok.Getter;

import java.util.regex.Pattern;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/13 9:45 下午
 */
public class RegexMatcher implements IStringMatcher {

    @JsonIgnore
    private final Pattern regex;

    @Getter
    private final String pattern;

    public RegexMatcher(@JsonProperty("pattern") String pattern) {
        this.regex = Pattern.compile(pattern);
        this.pattern = pattern;
    }

    @Override
    public boolean matches(String input) {
        return regex.matcher(input).matches();
    }

    @Override
    public <T> T accept(IMatcherVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
