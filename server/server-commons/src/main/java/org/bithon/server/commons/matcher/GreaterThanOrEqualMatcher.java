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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * @author frank.chen021@outlook.com
 * @date 9/5/22 1:41 PM
 */
public class GreaterThanOrEqualMatcher implements IMatcher {
    @Getter
    private final Number value;

    @JsonCreator
    public GreaterThanOrEqualMatcher(@JsonProperty("value") Number value) {
        this.value = value;
    }

    @Override
    public boolean matches(Object input) {
        if (input instanceof Integer || input instanceof Long) {
            return ((Number) input).longValue() >= value.longValue();
        }
        return ((Number) input).doubleValue() >= value.doubleValue();
    }

    @Override
    public <T> T accept(IMatcherVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
