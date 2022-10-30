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

package org.bithon.server.web.service.datasource.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/10/30 15:40
 */
@Getter
public class QueryColumn {

    /**
     * the output name
     */
    private final String name;

    /**
     * the internal name in storage layer
     */
    @Nullable
    private final String field;

    /**
     * Expression and aggregator properties are mutually exclusive.
     * If both of them are provided, expression will take effect.
     */
    @Nullable
    private final String expression;

    @Nullable
    private final String aggregator;

    @JsonCreator
    public QueryColumn(@JsonProperty("name") String name,
                       @JsonProperty("field") String field,
                       @JsonProperty("aggregator") String aggregator,
                       @JsonProperty("expression") String expression) {
        this.name = name;
        this.field = field == null ? name : field;
        this.aggregator = aggregator;
        this.expression = expression;
    }

    @JsonCreator
    public static QueryColumn fromString(String str) {
        return new QueryColumn(str, str, null, null);
    }
}
