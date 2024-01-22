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

package org.bithon.server.sink.tracing;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.sink.common.BatchConfig;
import org.bithon.server.sink.tracing.mapping.TraceIdMappingConfig;
import org.bithon.server.sink.tracing.sanitization.SanitizerConfig;
import org.bithon.server.storage.datasource.input.filter.AndFilter;
import org.bithon.server.storage.datasource.input.filter.IInputRowFilter;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 *  * <pre>
 *  *     bithon:
 *  *       processor:
 *  *         trace:
 *  *           transform:
 *  *             - type: filter
 *  *             - type: sanitize
 *  *           sink:
 *  *             - type: store
 *  *             -
 *  * </pre>
 *
 * @author frank.chen021@outlook.com
 * @date 10/12/21 3:33 PM
 */
@Data
@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "bithon.sinks.tracing")
public class TraceSinkConfig {

    private boolean enabled = true;

    private List<Map<String, String>> transformers;

    /**
     * Map<String, String>
     * key: prop name
     * val: prop values
     * <p>
     * The props must be complied with {@link IInputRowFilter}
     */
    private List<Map<String, String>> filters;

    private List<Map<String, String>> transform;

    private List<TraceIdMappingConfig> mapping;

    private SanitizerConfig globalSanitizer;
    private Map<String, SanitizerConfig> applicationSanitizer;

    private BatchConfig batch;

    @Nullable
    public IInputRowFilter createFilter(ObjectMapper om) {
        if (CollectionUtils.isEmpty(filters)) {
            return null;
        }

        List<IInputRowFilter> filterList = new ArrayList<>(filters.size());
        for (Map<String, String> filter : filters) {
            try {
                filterList.add(om.readValue(om.writeValueAsBytes(filter), IInputRowFilter.class));
            } catch (IOException ignored) {
            }
        }
        return filterList.isEmpty() ? null : new AndFilter(filterList);
    }

    @Nullable
    public ITransformer createTransformers(ObjectMapper om) {
        if (CollectionUtils.isEmpty(transformers)) {
            return null;
        }

        final List<ITransformer> transformerList = new ArrayList<>(transformers.size());
        for (Map<String, String> transformer : transformers) {
            try {
                transformerList.add(om.readValue(om.writeValueAsBytes(transformer), ITransformer.class));
            } catch (IOException ignored) {
            }
        }
        if (transformerList.isEmpty()) {
            return null;
        }

        return inputRow -> {
            for (ITransformer transformer : transformerList) {
                transformer.transform(inputRow);
            }
        };
    }
}
