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

package org.bithon.server.metric.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.common.matcher.IStringMatcher;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 4:43 下午
 */
public class DimensionCondition {

    @NotNull
    @Getter
    private final String dimension;

    @NotNull
    @Getter
    private final IStringMatcher matcher;

    @JsonCreator
    public DimensionCondition(@JsonProperty("dimension") String dimension,
                              @JsonProperty("matcher") IStringMatcher matcher) {
        this.dimension = dimension;
        this.matcher = matcher;
    }
}
