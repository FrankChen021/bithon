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

import java.util.Collection;
import java.util.List;

/**
 * @author Frank Chen
 * @date 1/11/21 2:50 pm
 */
@Data
public class TimeseriesQuery {
    private final DataSourceSchema dataSource;

    private final List<String> metrics;

    private final Collection<IFilter> filters;
    private final Interval interval;

    /**
     * time series also have groupBy, in this case, there will be multiple series
     */
    private final List<String> groupBys;

    public TimeseriesQuery(DataSourceSchema dataSource,
                           List<String> metrics,
                           Collection<IFilter> filters,
                           Interval interval,
                           List<String> groupBys) {
        this.dataSource = dataSource;
        this.metrics = metrics;
        this.filters = filters;
        this.interval = interval;
        this.groupBys = groupBys;
    }
}
