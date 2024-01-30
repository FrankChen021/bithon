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

import lombok.Builder;
import lombok.Data;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.query.ast.ResultColumn;
import org.bithon.server.storage.metrics.Interval;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Frank Chen
 * @date 1/11/21 2:50 pm
 */
@Data
@Builder
public class Query {
    private final ISchema schema;

    private final List<ResultColumn> resultColumns;

    private final IExpression filter;
    private final Interval interval;

    private final List<String> groupBy;

    @Nullable
    private final OrderBy orderBy;

    @Nullable
    private final Limit limit;

    private final ResultFormat resultFormat;

    public enum ResultFormat {
        /**
         * Object is output as an array
         */
        ValueArray,
        Object
    }

    public Query(ISchema schema,
                 List<ResultColumn> resultColumns,
                 IExpression filter,
                 Interval interval,
                 @Nullable List<String> groupBy,
                 @Nullable OrderBy orderBy,
                 @Nullable Limit limit,
                 @Nullable ResultFormat resultFormat) {
        this.schema = schema;
        this.resultColumns = resultColumns;
        this.filter = filter;
        this.interval = interval;
        this.groupBy = CollectionUtils.emptyOrOriginal(groupBy);
        this.orderBy = orderBy;
        this.limit = limit;
        this.resultFormat = resultFormat == null ? ResultFormat.Object : resultFormat;
    }
}
