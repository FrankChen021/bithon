package org.bithon.server.common.matcher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import javax.validation.constraints.NotNull;

/**
 * @author Frank Chen
 * @date 8/1/22 2:41 PM
 */
public class NotEqualMatcher  implements IStringMatcher {
    @Getter
    @NotNull
    private final String pattern;

    @JsonCreator
    public NotEqualMatcher(@JsonProperty("pattern") @NotNull String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(String input) {
        return !pattern.equals(input);
    }

    @Override
    public <T> T accept(IMatcherVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
