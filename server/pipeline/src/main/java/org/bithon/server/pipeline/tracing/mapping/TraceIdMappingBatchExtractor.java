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

package org.bithon.server.pipeline.tracing.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.pipeline.tracing.TracePipelineConfig;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 10/12/21 4:44 PM
 */
@Slf4j
public class TraceIdMappingBatchExtractor {

    private List<ITraceIdMappingExtractor> extractorList;

    public TraceIdMappingBatchExtractor(List<ITraceIdMappingExtractor> extractorList) {
        this.extractorList = extractorList;
    }

    private void setExtractorList(List<ITraceIdMappingExtractor> extractorList) {
        this.extractorList = extractorList;
    }

    public List<TraceIdMapping> extract(Collection<TraceSpan> spanList) {
        Set<String> duplication = new HashSet<>();

        // remove duplicated mappings from returned values of extractors
        List<TraceIdMapping> mappings = new ArrayList<>();

        for (TraceSpan span : spanList) {
            // extractors extract mapping from tags,
            // if there are no tags, it's no need to call extractors
            if (CollectionUtils.isEmpty(span.getTags())) {
                continue;
            }

            for (ITraceIdMappingExtractor extractor : extractorList) {
                extractor.extract(span,
                                  (thisSpan, userId) -> {
                                      if (duplication.add(thisSpan.getTraceId() + "/" + userId)) {
                                          mappings.add(new TraceIdMapping(userId,
                                                                          thisSpan.getStartTime() / 1000,
                                                                          thisSpan.getTraceId()));

                                      }
                                  });
            }
        }

        return mappings;
    }

    /**
     * create trace id mapping extractors from configuration
     */
    public static TraceIdMappingBatchExtractor create(ConfigurableApplicationContext applicationContext) {
        //
        // Create extractors from configuration
        //
        TracePipelineConfig config = applicationContext.getBean(TracePipelineConfig.class);
        TraceIdMappingBatchExtractor factory = new TraceIdMappingBatchExtractor(deserializeExtractors(applicationContext.getBean(ObjectMapper.class), config));

        //
        // Register a listener to reload extractors when configuration changed
        //
        ConfigurationProperties properties = TracePipelineConfig.class.getAnnotation(ConfigurationProperties.class);
        Preconditions.checkNotNull(properties, "PipelineConfig class must be annotated with @ConfigurationProperties");
        String pipelineConfigPrefix = properties.prefix();
        String prefix = properties.prefix() + ".mapping";
        applicationContext.addApplicationListener((ApplicationListener<EnvironmentChangeEvent>) event -> {
            boolean isMappingChanged = event.getKeys()
                                            .stream()
                                            .anyMatch(key -> key.startsWith(prefix));
            if (!isMappingChanged) {
                return;
            }

            TracePipelineConfig pipelineConfig = Binder.get(applicationContext.getEnvironment())
                                                       .bind(pipelineConfigPrefix, TracePipelineConfig.class)
                                                       .orElse(null);
            if (pipelineConfig == null) {
                log.warn("Trace mapping configuration changed. But failed to reload configurations.");
                return;
            }

            ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);
            try {
                String json = pipelineConfig.getMapping() == null ? "[]" : objectMapper.copy()
                                                                                       .configure(SerializationFeature.INDENT_OUTPUT, true)
                                                                                       .writeValueAsString(pipelineConfig.getMapping());
                log.info("Reloading trace id mappings:\n{}", json);
            } catch (JsonProcessingException ignored) {
            }

            factory.setExtractorList(deserializeExtractors(objectMapper, pipelineConfig));
        });

        return factory;
    }

    private static List<ITraceIdMappingExtractor> deserializeExtractors(ObjectMapper objectMapper, TracePipelineConfig config) {
        List extractors = initializeExtractors();

        if (CollectionUtils.isEmpty(config.getMapping())) {
            return extractors;
        }

        for (TraceIdMappingConfig mappingConfig : config.getMapping()) {
            try {
                String json = objectMapper.writeValueAsString(mappingConfig);
                extractors.add(objectMapper.readValue(json, ITraceIdMappingExtractor.class));
            } catch (IOException e) {
                throw new RuntimeException("Unable to create extractor for type " + mappingConfig.getOrDefault("type", ""), e);
            }
        }

        return extractors;
    }

    /**
     * Only for test cases
     */
    @VisibleForTesting
    static TraceIdMappingBatchExtractor create(ITraceIdMappingExtractor... extractors) {
        List<ITraceIdMappingExtractor> extractorList = initializeExtractors();
        extractorList.addAll(Arrays.asList(extractors));
        return new TraceIdMappingBatchExtractor(extractorList);
    }

    private static List<ITraceIdMappingExtractor> initializeExtractors() {
        // Add default extractor to first
        List extractors = new ArrayList();
        extractors.add(CurrentTraceIdExtractor.INSTANCE);
        extractors.add(UpstreamTraceIdExtractor.INSTANCE);
        return extractors;
    }
}
