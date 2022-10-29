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

package org.bithon.server.web.service.api;

import org.bithon.server.storage.datasource.DataSourceSchema;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/11/08 8:19 下午
 */
public interface IDataSourceApi {

    @PostMapping("/api/datasource/timeseries/v2")
    DataSourceService.TimeSeriesQueryResult timeseries(@Validated @RequestBody TimeSeriesQueryRequest request);

    @PostMapping("/api/datasource/groupBy")
    List<Map<String, Object>> groupBy(@Validated @RequestBody GroupByQueryRequest request);

    @PostMapping("/api/datasource/list")
    ListQueryResponse list(@Validated @RequestBody ListQueryRequest request);

    /**
     * A unified interface that will supersede {@link #groupBy(GroupByQueryRequest)} and {@link #list(ListQueryRequest)}
     */
    @PostMapping("/api/datasource/query")
    List<Map<String, Object>> query(@Validated @RequestBody GeneralQueryRequest request);
    
    @PostMapping("/api/datasource/schemas")
    Map<String, DataSourceSchema> getSchemas();

    @PostMapping("/api/datasource/schema/{name}")
    DataSourceSchema getSchemaByName(@PathVariable("name") String schemaName);

    @PostMapping("/api/datasource/schema/create")
    void createSchema(@RequestBody DataSourceSchema schema);

    @PostMapping("/api/datasource/schema/update")
    void updateSchema(@RequestBody DataSourceSchema schema);

    @PostMapping("/api/datasource/name")
    Collection<DisplayableText> getSchemaNames();

    @Deprecated
    @PostMapping("/api/datasource/dimensions")
    Collection<Map<String, String>> getDimensions(@Validated @RequestBody GetDimensionRequest request);

    @PostMapping("/api/datasource/dimensions/v2")
    Collection<Map<String, String>> getDimensionsV2(@Validated @RequestBody GetDimensionRequestV2 request);

    @PostMapping("api/datasource/ttl/update")
    void updateSpecifiedDataSourceTTL(@RequestBody UpdateTTLRequest request);
}
