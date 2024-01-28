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

package org.bithon.server.web.service.datasource.api;

import lombok.Data;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.IDataSource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.Min;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/11/08 8:19 下午
 */
public interface IDataSourceApi {

    @PostMapping("/api/datasource/timeseries/v3")
    GeneralQueryResponse timeseriesV3(@Validated @RequestBody GeneralQueryRequest request) throws IOException;

    @PostMapping("/api/datasource/timeseries/v4")
    GeneralQueryResponse timeseriesV4(@Validated @RequestBody GeneralQueryRequest request) throws IOException;

    @PostMapping("/api/datasource/groupBy/v2")
    GeneralQueryResponse groupBy(@Validated @RequestBody GeneralQueryRequest request) throws IOException;

    @PostMapping("/api/datasource/groupBy/v3")
    GeneralQueryResponse groupByV3(@Validated @RequestBody GeneralQueryRequest request) throws IOException;

    @PostMapping("/api/datasource/list/v2")
    GeneralQueryResponse list(@Validated @RequestBody GeneralQueryRequest request) throws IOException;

    @PostMapping("/api/datasource/schemas")
    Map<String, IDataSource> getSchemas();

    @PostMapping("/api/datasource/schema/{name}")
    IDataSource getSchemaByName(@PathVariable("name") String schemaName);

    @PostMapping("/api/datasource/schema/create")
    void createSchema(@RequestBody DataSourceSchema schema);

    @PostMapping("/api/datasource/schema/update")
    void updateSchema(@RequestBody DataSourceSchema schema);

    @PostMapping("/api/datasource/name")
    Collection<DisplayableText> getSchemaNames();

    /**
     * Get distinct values of a specific column under given condition
     */
    @PostMapping("/api/datasource/dimensions/v2")
    Collection<Map<String, String>> getDimensions(@Validated @RequestBody GetDimensionRequest request) throws IOException;

    @PostMapping("/api/datasource/ttl/update")
    void updateSpecifiedDataSourceTTL(@RequestBody UpdateTTLRequest request);

    @Data
    class SaveMetricBaselineRequest {
        private String date;

        @Min(0)
        private int keepDays = 0;
    }

    @PostMapping("/api/metric/baseline/save")
    void saveMetricBaseline(@Validated @RequestBody SaveMetricBaselineRequest request);

    @PostMapping("/api/metric/baseline/get")
    List<String> getBaselineDate();
}
