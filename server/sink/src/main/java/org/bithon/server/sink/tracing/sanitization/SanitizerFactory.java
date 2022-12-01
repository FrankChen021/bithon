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

package org.bithon.server.sink.tracing.sanitization;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.sink.tracing.TraceSinkConfig;
import org.bithon.server.storage.tracing.TraceSpan;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 10/1/22 2:28 PM
 */
@Slf4j
public class SanitizerFactory {
    private final ObjectMapper objectMapper;
    private final Map<String, ISanitizer> applicationSanitizers;
    private final ISanitizer globalSanitizer;

    public SanitizerFactory(ObjectMapper objectMapper, TraceSinkConfig traceConfig) {
        this.objectMapper = objectMapper;
        this.applicationSanitizers = new HashMap<>();
        if (traceConfig.getApplicationSanitizer() != null) {
            traceConfig.getApplicationSanitizer().forEach((application, config) -> {
                ISanitizer sanitizer = getSanitizer(config);
                if (sanitizer != null) {
                    applicationSanitizers.put(application, sanitizer);
                }
            });
        }
        this.globalSanitizer = traceConfig.getGlobalSanitizer() == null ? null : getSanitizer(traceConfig.getGlobalSanitizer());
    }

    private ISanitizer getSanitizer(SanitizerConfig config) {
        try {
            //flatten the configuration
            Map<String, Object> map = new HashMap<>();
            map.put("type", config.getType());
            map.putAll(config.getArgs());
            String json = objectMapper.writeValueAsString(map);

            return objectMapper.readValue(json, ISanitizer.class);
        } catch (IOException e) {
            log.error("Unable to create extractor for type " + config.getType(), e);
            return null;
        }
    }

    public void sanitize(List<TraceSpan> spans) {
        if (applicationSanitizers.isEmpty() && globalSanitizer == null) {
            return;
        }
        for (TraceSpan span : spans) {
            sanitize(span);
        }
    }

    private void sanitize(TraceSpan span) {
        if (span.getAppName() != null) {
            ISanitizer sanitizer = applicationSanitizers.get(span.getAppName());
            if (sanitizer != null) {
                sanitizer.sanitize(span);
            }
        }
        if (globalSanitizer != null) {
            globalSanitizer.sanitize(span);
        }
    }
}
