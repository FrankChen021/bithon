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
import lombok.Data;
import lombok.Getter;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.storage.metrics.Limit;
import org.bithon.server.storage.metrics.OrderBy;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;

/**
 * @author Frank Chen
 * @date 29/10/22 9:04 pm
 */
@Data
public class GeneralQueryRequest {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = QueryColumn.class)
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
    public static class QueryColumn extends AbstractQueryColumn {
        @Nullable
        private final String aggregator;

        @Nullable
        private final String field;

        @JsonCreator
        public QueryColumn(@JsonProperty("name") String name,
                           @JsonProperty("aggregator") String aggregator,
                           @JsonProperty("field") String field) {
            super(name);
            this.aggregator = aggregator;
            this.field = field;
        }

        @JsonCreator
        public static QueryColumn fromString(String str) {
            return new QueryColumn(str, null, null);
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

    @NotEmpty
    private String type;

    @NotEmpty
    private String dataSource;

    @NotNull
    @Valid
    private IntervalRequest interval;

    private Collection<IFilter> filters;

    @NotEmpty
    private List<AbstractQueryColumn> columns;

    @Nullable
    private OrderBy orderBy;

    @Nullable
    private Limit limit;
}
