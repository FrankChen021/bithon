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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.server.storage.datasource.query.Limit;
import org.bithon.server.storage.datasource.query.OrderBy;
import org.bithon.server.storage.datasource.query.Query;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Frank Chen
 * @date 29/10/22 9:04 pm
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {

    @NotEmpty
    private String dataSource;

    @NotNull
    @Valid
    private IntervalRequest interval;

    @Nullable
    private String filterExpression;

    @NotEmpty
    private List<QueryField> fields;

    /**
     * Use LinkedHashSet to keep the order of the client side
     */
    @Nullable
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<String> groupBy;

    @Nullable
    private OrderBy orderBy;

    @Valid
    @Nullable
    private Limit limit;

    private Query.ResultFormat resultFormat;
}
