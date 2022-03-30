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

package org.bithon.server.metric.storage;

import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.metric.DataSourceSchema;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 11:09 上午
 */
public interface IMetricReader {

    List<Map<String, Object>> timeseries(TimeseriesQuery query);

    /**
     * Aggregate metrics by their pre-defined aggregators in the given period
     */
    List<Map<String, Object>> groupBy(GroupByQuery groupByQuery);

    List<Map<String, Object>> list(ListQuery listQuery);
    int listSize(ListQuery listQuery);

    List<Map<String, Object>> executeSql(String sql);

    List<Map<String, String>> getDimensionValueList(TimeSpan start,
                                                    TimeSpan end,
                                                    DataSourceSchema dataSourceSchema,
                                                    Collection<IFilter> dimensions,
                                                    String dimension);
}
