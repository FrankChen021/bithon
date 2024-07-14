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

package org.bithon.server.pipeline.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.server.pipeline.metrics.input.IMetricInputSource;
import org.bithon.server.storage.datasource.ISchema;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/7/14 13:02
 */
@RestController
public class PipelineApi implements IPipelineApi {

    private final ObjectMapper objectMapper;

    public PipelineApi(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Map<String, Object>> test(ISchema schema) {
        IMetricInputSource inputSource = objectMapper.convertValue(schema.getInputSourceSpec(),
                                                                   // Use ths stub input source
                                                                   IMetricInputSource.class);
        inputSource.start(schema);
        inputSource.stop();
        // deserialize the input source
        // start the input source
        // wait for a while
        // stop the input source
        return List.of();
    }
}
