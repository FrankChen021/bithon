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

package org.bithon.server.storage.datasource.query;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.query.ast.Selector;
import org.bithon.server.storage.metrics.Interval;

import java.util.List;

/**
 * @author Frank Chen
 * @date 1/11/21 2:50 pm
 */
@Data
@Builder
public class Query {
    private final ISchema schema;

    private final List<Selector> selectors;

    private final IExpression filter;
    private final Interval interval;

    private final List<String> groupBy;

    @Nullable
    private final OrderBy orderBy;

    @Nullable
    private final Limit limit;

    private final HumanReadableDuration offset;

    private final ResultFormat resultFormat;

    /**
     * Was used, but not used now. May be used in future.
     */
    public enum ResultFormat {
        /**
         * Object is output as an array
         */
        ValueArray,
        Object
    }

    public Query(ISchema schema,
                 List<Selector> selectors,
                 IExpression filter,
                 Interval interval,
                 @Nullable List<String> groupBy,
                 @Nullable OrderBy orderBy,
                 @Nullable Limit limit,
                 @Nullable HumanReadableDuration offset,
                 @Nullable ResultFormat resultFormat) {
        this.schema = schema;
        this.selectors = selectors;
        this.filter = filter;
        this.interval = interval;
        this.groupBy = CollectionUtils.emptyOrOriginal(groupBy);
        this.orderBy = orderBy;
        this.offset = offset;
        this.limit = limit;
        this.resultFormat = resultFormat == null ? ResultFormat.Object : resultFormat;
    }

    /**
     * Create a new query object based on an existing query.
     * All fields in the new object except for {@param filter} is a shallow copy of an existing query object.
     */
    public Query with(IExpression filter) {
        return new Query(this.schema,
                         this.selectors,
                         filter,
                         this.interval,
                         this.groupBy,
                         this.orderBy,
                         this.limit,
                         this.offset,
                         this.resultFormat);
    }
}
