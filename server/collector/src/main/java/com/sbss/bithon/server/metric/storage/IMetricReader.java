/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.metric.storage;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sbss.bithon.server.common.utils.datetime.TimeSpan;
import com.sbss.bithon.server.metric.DataSourceSchema;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 11:09 上午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    //@JsonSubTypes.Type(name = "druid", value = DruidStorageReader.class)
})
public interface IMetricReader {

    /**
     * TODO: Actually, this method is a time-series implementation. Should be renamed some day
     */
    List<Map<String, Object>> timeseries(TimeSpan start,
                                         TimeSpan end,
                                         DataSourceSchema dataSourceSchema,
                                         Collection<DimensionCondition> filter,
                                         Collection<String> metrics,
                                         int interval);

    /**
     * Aggregate metrics by their pre-defined aggregators in the given period
     */
    Map<String, Object> getMetricValue(TimeSpan start,
                                       TimeSpan end,
                                       DataSourceSchema dataSourceSchema,
                                       Collection<DimensionCondition> dimensions,
                                       Collection<String> metrics);

    /**
     * Aggregate metrics by their pre-defined aggregators in the given period
     *
     */
    List<Map<String, Object>> groupBy(TimeSpan start,
                                      TimeSpan end,
                                      DataSourceSchema dataSourceSchema,
                                      Collection<DimensionCondition> filter,
                                      Collection<String> metrics,
                                      Collection<String> groupBy);

    List<Map<String, Object>> executeSql(String sql);

    List<Map<String, String>> getDimensionValueList(TimeSpan start,
                                                    TimeSpan end,
                                                    DataSourceSchema dataSourceSchema,
                                                    Collection<DimensionCondition> dimensions,
                                                    String dimension);
}
