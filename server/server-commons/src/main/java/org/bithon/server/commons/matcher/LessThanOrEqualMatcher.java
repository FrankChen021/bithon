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
import org.bithon.component.commons.utils.Preconditions;

/**
 * @author frank.chen021@outlook.com
 * @date 9/5/22 1:42 PM
 */
public class LessThanOrEqualMatcher implements IMatcher {

    @Getter
    private final Object value;

    @JsonCreator
    public LessThanOrEqualMatcher(@JsonProperty("value") Object value) {
        Preconditions.checkNotNull(value, "value can't be null.");
        Preconditions.checkIfTrue((value instanceof Number) || (value instanceof String), "value must be type of number or string.");
        this.value = value;
    }

    @Override
    public boolean matches(Object input) {
        if (input instanceof Number) {
            if (input instanceof Integer || input instanceof Long) {
                return ((Number) input).longValue() <= ((Number) value).longValue();
            }
            return ((Number) input).doubleValue() <= ((Number) value).doubleValue();
        }
        return input.toString().compareTo(value.toString()) <= 0;
    }

    @Override
    public <T> T accept(IMatcherVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
