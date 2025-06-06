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

package org.bithon.server.pipeline.tracing.sampler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.pipeline.common.input.IInputSource;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/7/15 21:02
 */
@RestController
@Conditional(TraceSamplingEnabler.class)
public class TraceSampler implements ITraceSampler {
    private final ObjectMapper objectMapper;

    public TraceSampler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<StreamingResponseBody> sample(ISchema schema) {
        if (schema.getInputSourceSpec() == null
            // or the input source is a null JSON node
            || schema.getInputSourceSpec().isNull()) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "Input source is not specified in the schema");
        }

        IInputSource inputSource = objectMapper.convertValue(schema.getInputSourceSpec(),
                                                             IInputSource.class);

        StreamingResponseBody body = inputSource.sample(schema, Duration.ofSeconds(10));

        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_NDJSON)
                             .body(body);
    }
}
