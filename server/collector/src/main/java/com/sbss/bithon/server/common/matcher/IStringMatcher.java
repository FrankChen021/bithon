package com.sbss.bithon.server.common.matcher;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/13 9:41 下午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "contains", value = ContainsMatcher.class),
    @JsonSubTypes.Type(name = "equal", value = EqualMatcher.class),
    @JsonSubTypes.Type(name = "icontains", value = IContainsMatcher.class),
    @JsonSubTypes.Type(name = "startwith", value = StartwithMatcher.class),
    @JsonSubTypes.Type(name = "endwith", value = EndwithMatcher.class),
    @JsonSubTypes.Type(name = "regex", value = RegexMatcher.class),
    @JsonSubTypes.Type(name = "antPath", value = AntPathMatcher.class),
})
public interface IStringMatcher {
    boolean matches(String input);

    <T> T accept(IMatcherVisitor<T> visitor);
}
