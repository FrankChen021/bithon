/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.common.matcher;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
