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

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.query.pipeline.ColumnarTable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/11/08 8:19 下午
 */
public interface IDataSourceApi {

    @PostMapping("/api/datasource/timeseries/v4")
    QueryResponse timeseriesV4(@Validated @RequestBody QueryRequest request) throws IOException;

    /**
     * Internal API that returns row based records for internal API use
     */
    @PostMapping("/api/internal/datasource/timeseries")
    ColumnarTable timeseriesV5(@Validated @RequestBody QueryRequest request) throws IOException;

    @PostMapping("/api/datasource/groupBy/v3")
    QueryResponse groupByV3(@Validated @RequestBody QueryRequest request) throws IOException;

    @PostMapping("/api/datasource/list/v2")
    QueryResponse list(@Validated @RequestBody QueryRequest request) throws IOException;

    @PostMapping("/api/datasource/schemas")
    Map<String, ISchema> getSchemas();

    @PostMapping("/api/datasource/schema/{name}")
    ISchema getSchemaByName(@PathVariable("name") String schemaName);

    /**
     * Test and sample data by using the input source defined in given schema
     */
    @PostMapping("/api/datasource/schema/test")
    ResponseEntity<StreamingResponseBody> testSchema(@RequestBody ISchema schema);

    @PostMapping("/api/datasource/schema/create")
    void createSchema(@RequestBody ISchema schema);

    @PostMapping("/api/datasource/schema/update")
    void updateSchema(@RequestBody ISchema schema);

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
