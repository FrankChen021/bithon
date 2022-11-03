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

package org.bithon.server.storage.metrics;

import lombok.Builder;
import lombok.Data;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.storage.datasource.DataSourceSchema;

import javax.annotation.Nullable;
import javax.validation.constraints.Null;
import java.util.Collection;
import java.util.List;

/**
 * @author Frank Chen
 * @date 1/11/21 2:50 pm
 */
@Data
@Builder
public class Query {
    private final DataSourceSchema dataSource;

    private final Collection<Object> fields;

    private final Collection<IFilter> filters;
    private final Interval interval;

    private final List<String> groupBy;

    @Nullable
    private final OrderBy orderBy;

    @Nullable
    private final Limit limit;

    private final ResultFormat resultFormat;

    public enum ResultFormat {
        ValueArray,
        Object
    }

    public Query(DataSourceSchema dataSource,
                 Collection<Object> fields,
                 Collection<IFilter> filters,
                 Interval interval,
                 @Nullable List<String> groupBy,
                 @Nullable OrderBy orderBy,
                 @Nullable Limit limit,
                 @Nullable ResultFormat resultFormat) {
        this.dataSource = dataSource;
        this.fields = CollectionUtils.emptyOrOriginal(fields);
        this.filters = filters;
        this.interval = interval;
        this.groupBy = CollectionUtils.emptyOrOriginal(groupBy);
        this.orderBy = orderBy;
        this.limit = limit;
        this.resultFormat = resultFormat == null ? ResultFormat.Object : resultFormat;
    }
}
