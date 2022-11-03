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
import org.bithon.server.storage.datasource.api.IQueryStageAggregator;
import org.bithon.server.storage.datasource.spec.PostAggregatorMetricSpec;

import javax.annotation.Nullable;
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

    private final List<String> metrics;
    private final List<IQueryStageAggregator> aggregators;

    private final List<PostAggregatorMetricSpec> postAggregators;
    private final Collection<IFilter> filters;
    private final Interval interval;

    private final List<String> groupBy;

    @Nullable
    private final OrderBy orderBy;

    @Nullable
    private final Limit limit;

    public Query(DataSourceSchema dataSource,
                 List<String> metrics,
                 @Nullable List<IQueryStageAggregator> aggregators,
                 @Nullable List<PostAggregatorMetricSpec> postAggregators,
                 Collection<IFilter> filters,
                 Interval interval,
                 @Nullable List<String> groupBy,
                 @Nullable OrderBy orderBy,
                 @Nullable Limit limit) {
        this.dataSource = dataSource;
        this.metrics = CollectionUtils.emptyOrOriginal(metrics);
        this.aggregators = CollectionUtils.emptyOrOriginal(aggregators);
        this.postAggregators = CollectionUtils.emptyOrOriginal(postAggregators);
        this.filters = filters;
        this.interval = interval;
        this.groupBy = CollectionUtils.emptyOrOriginal(groupBy);
        this.orderBy = orderBy;
        this.limit = limit;
    }
}
