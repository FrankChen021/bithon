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

import lombok.Data;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.api.IQueryableAggregator;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 1/11/21 2:50 pm
 */
@Data
public class GroupByQuery {
    private final DataSourceSchema dataSource;

    private final List<String> metrics;
    private final List<IQueryableAggregator> aggregators;

    private final Collection<IFilter> filters;
    private final Interval interval;

    private final List<String> groupBys;
    private final OrderBy orderBy;

    public GroupByQuery(DataSourceSchema dataSource,
                        List<String> metrics,
                        List<IQueryableAggregator> aggregators,
                        Collection<IFilter> filters,
                        Interval interval,
                        List<String> groupBys,
                        @Nullable OrderBy orderBy) {
        this.dataSource = dataSource;
        this.metrics = metrics;
        this.aggregators = aggregators;
        this.filters = filters;
        this.interval = interval;
        this.groupBys = groupBys;
        this.orderBy = orderBy;
    }
}
