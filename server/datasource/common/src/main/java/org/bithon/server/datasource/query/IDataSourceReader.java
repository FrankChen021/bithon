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

package org.bithon.server.datasource.query;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.datasource.query.plan.logical.LogicalTableScan;
import org.bithon.server.datasource.query.plan.physical.IPhysicalPlan;
import org.bithon.server.datasource.query.result.ColumnarTable;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 11:09 上午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IDataSourceReader extends AutoCloseable {

    default IPhysicalPlan plan(LogicalTableScan tableScan, Interval interval) {
        throw new UnsupportedOperationException("This data source does not support logical plan execution");
    }

    ColumnarTable timeseries(Query query);

    /**
     * Aggregate metrics by their pre-defined aggregators in the given period
     * The returned list is a list of map, for each map object, the key of the map is the column name
     */
    List<?> groupBy(Query query);

    List<?> select(Query query);

    int count(Query query);

    List<String> distinct(Query query);

    default void close() {
    }
}
