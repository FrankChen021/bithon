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

package org.bithon.agent.observability.utils.filter;

import org.bithon.shaded.com.fasterxml.jackson.annotation.JsonCreator;
import org.bithon.shaded.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/11 18:15
 */
public class StringEqualMatcher implements IMatcher {

    public static final String TYPE = "==";
    private final String pattern;

    @JsonCreator
    public StringEqualMatcher(@JsonProperty("pattern") String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(Object input) {
        return pattern.matches(input.toString());
    }
}
