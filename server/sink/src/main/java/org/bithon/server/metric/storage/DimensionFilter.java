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
import org.bithon.server.common.matcher.IMatcher;

import javax.annotation.Nullable;

/**
 * @author Frank Chen
 * @date 25/3/22 5:15 PM
 */
@Getter
public class DimensionFilter implements IFilter {
    private final IMatcher matcher;
    private final String name;
    private final String nameType;

    @JsonCreator
    public DimensionFilter(@JsonProperty("dimension") String name,
                           @JsonProperty("nameType") @Nullable String nameType,
                           @JsonProperty("matcher") IMatcher matcher) {
        this.name = name;
        this.nameType = nameType == null ? "name" : "alias";
        this.matcher = matcher;
    }

    public DimensionFilter(String dimension, IMatcher matcher) {
        this(dimension, null, matcher);
    }

    @Override
    public String getType() {
        return "dimension";
    }
}
