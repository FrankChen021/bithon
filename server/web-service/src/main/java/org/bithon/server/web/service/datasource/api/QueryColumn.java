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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/10/30 15:40
 */
public class QueryColumn {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = DefaultQueryColumn.class)
    @JsonSubTypes(value = {
        @JsonSubTypes.Type(name = "expression", value = ExpressionQueryColumn.class)
    })
    @Getter
    public abstract static class AbstractQueryColumn {
        protected final String name;

        protected AbstractQueryColumn(String name) {
            this.name = name;
        }
    }

    @Getter
    public static class DefaultQueryColumn extends AbstractQueryColumn {
        @Nullable
        private final String aggregator;

        @Nullable
        private final String field;

        @JsonCreator
        public DefaultQueryColumn(@JsonProperty("name") String name,
                                  @JsonProperty("aggregator") String aggregator,
                                  @JsonProperty("field") String field) {
            super(name);
            this.aggregator = aggregator;
            this.field = field;
        }

        @JsonCreator
        public static DefaultQueryColumn fromString(String str) {
            return new DefaultQueryColumn(str, null, null);
        }
    }

    @Getter
    public static class ExpressionQueryColumn extends AbstractQueryColumn {
        @Nullable
        private final String expression;

        @Nullable
        private final String field;

        @JsonCreator
        public ExpressionQueryColumn(@JsonProperty("name") String name,
                                     @JsonProperty("expression") String expression,
                                     @JsonProperty("field") String field) {
            super(name);
            this.expression = expression;
            this.field = field;
        }
    }
}
