/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.commons.matcher;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/13 9:41 下午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "contains", value = StringContainsMatcher.class),
    @JsonSubTypes.Type(name = "equal", value = StringEqualMatcher.class),
    @JsonSubTypes.Type(name = "notEqual", value = NotEqualMatcher.class),
    @JsonSubTypes.Type(name = "icontains", value = StringIContainsMatcher.class),
    @JsonSubTypes.Type(name = "startwith", value = StringStartsWithMatcher.class),
    @JsonSubTypes.Type(name = "endwith", value = StringEndWithMatcher.class),
    @JsonSubTypes.Type(name = "regex", value = StringRegexMatcher.class),
    @JsonSubTypes.Type(name = "antPath", value = StringAntPathMatcher.class),
    @JsonSubTypes.Type(name = "between", value = BetweenMatcher.class),
    @JsonSubTypes.Type(name = "in", value = InMatcher.class),
    @JsonSubTypes.Type(name = "gt", value = GreaterThanMatcher.class),
    @JsonSubTypes.Type(name = "gte", value = GreaterThanOrEqualMatcher.class),
    @JsonSubTypes.Type(name = "lt", value = LessThanMatcher.class),
    @JsonSubTypes.Type(name = "lte", value = LessThanOrEqualMatcher.class),
})
public interface IMatcher {
    boolean matches(Object input);

    <T> T accept(IMatcherVisitor<T> visitor);
}
