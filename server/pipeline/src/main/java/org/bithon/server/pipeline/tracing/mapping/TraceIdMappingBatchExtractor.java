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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.pipeline.tracing.TracePipelineConfig;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author frank.chen021@outlook.com
 * @date 10/12/21 4:44 PM
 */
@Slf4j
public class TraceIdMappingBatchExtractor {

    /**
     * create trace id mapping extractors from configuration
     */
    public static Function<Collection<TraceSpan>, List<TraceIdMapping>> create(ApplicationContext context) {
        final List<ITraceIdMappingExtractor> extractorList = new ArrayList<>();

        //
        // Create extractors from configuration
        //
        TracePipelineConfig config = context.getBean(TracePipelineConfig.class);
        if (!CollectionUtils.isEmpty(config.getMapping())) {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            for (TraceIdMappingConfig mappingConfig : config.getMapping()) {
                try {
                    String json = mapper.writeValueAsString(mappingConfig);
                    extractorList.add(mapper.readValue(json, ITraceIdMappingExtractor.class));
                } catch (IOException e) {
                    throw new RuntimeException("Unable to create extractor for type " + mappingConfig.getOrDefault("type", ""), e);
                }
            }
        }

        return create(extractorList);
    }

    /**
     * Only for test cases
     */
    static Function<Collection<TraceSpan>, List<TraceIdMapping>> create(ITraceIdMappingExtractor... extractors) {
        return create(new ArrayList<>(Arrays.asList(extractors)));
    }

    static Function<Collection<TraceSpan>, List<TraceIdMapping>> create(List<ITraceIdMappingExtractor> extractorList) {
        // Add default extractor to first
        extractorList.add(0, CompatibilityIdMappingExtractor.INSTANCE);
        extractorList.add(0, TraceIdExtractor.INSTANCE);

        return spanList -> {
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
                                      (thisSpan, uTxId) -> {
                                          if (duplication.add(uTxId)) {
                                              mappings.add(new TraceIdMapping(uTxId,
                                                                              thisSpan.getStartTime() / 1000,
                                                                              thisSpan.getTraceId()));

                                          }
                                      });
                }
            }

            return mappings;
        };
    }

    /**
     * see: <a href="https://github.com/FrankChen021/bithon/issues/260">This issue</a>
     */
    static class CompatibilityIdMappingExtractor implements ITraceIdMappingExtractor {
        static final ITraceIdMappingExtractor INSTANCE = new CompatibilityIdMappingExtractor();

        @Override
        public void extract(TraceSpan span, BiConsumer<TraceSpan, String> consumer) {
            if (!"SERVER".equals(span.getKind())) {
                return;
            }
            String upstreamTraceId = span.getTags().get("upstreamTraceId");
            if (StringUtils.hasText(upstreamTraceId)) {
                consumer.accept(span, upstreamTraceId);
            }
        }
    }

    /**
     * Extract trace id itself as a mapping-entry.
     * This helps to improve the search performance when searching trace by given trace-id
     */
    static class TraceIdExtractor implements ITraceIdMappingExtractor {
        static final ITraceIdMappingExtractor INSTANCE = new TraceIdExtractor();

        @Override
        public void extract(TraceSpan span, BiConsumer<TraceSpan, String> consumer) {
            consumer.accept(span, span.getTraceId());
        }
    }
}
