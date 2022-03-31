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

package org.bithon.server.sink.tracing.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.sink.tracing.TraceConfig;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author Frank Chen
 * @date 10/12/21 4:44 PM
 */
@Slf4j
public class TraceMappingFactory {

    /**
     * create trace id mapping extractors from configuration
     */
    public static Function<Collection<TraceSpan>, List<TraceIdMapping>> create(ApplicationContext context) {
        final List<ITraceIdMappingExtractor> extractorList = new ArrayList<>();

        // add default extractor
        extractorList.add(CompatibilityIdMappingExtractor.INSTANCE);

        //
        // create extractors from configuration
        //
        TraceConfig config = context.getBean(TraceConfig.class);
        if (!CollectionUtils.isEmpty(config.getMapping())) {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            for (TraceIdMappingConfig mappingConfig : config.getMapping()) {
                try {
                    // flatten the configuration
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", mappingConfig.getType());
                    map.putAll(mappingConfig.getArgs());
                    String json = mapper.writeValueAsString(map);
                    extractorList.add(mapper.readValue(json, ITraceIdMappingExtractor.class));
                } catch (IOException e) {
                    log.error("Unable to create extractor for type " + mappingConfig.getType(), e);
                }
            }
        }

        return create(extractorList);
    }

    /**
     * only for test cases
     */
    static Function<Collection<TraceSpan>, List<TraceIdMapping>> create(ITraceIdMappingExtractor extractor) {
        return create(Arrays.asList(CompatibilityIdMappingExtractor.INSTANCE, extractor));
    }

    static Function<Collection<TraceSpan>, List<TraceIdMapping>> create(List<ITraceIdMappingExtractor> extractorList) {
        return spanList -> {
            Set<String> duplication = new HashSet<>();

            // remove duplicated mappings from returned values of extractors
            List<TraceIdMapping> mappings = new ArrayList<>();

            for (TraceSpan span : spanList) {
                // extractors extract mapping from tags,
                // if there's no tags, it's no need to call extractors
                if (CollectionUtils.isEmpty(span.getTags())) {
                    continue;
                }

                for (ITraceIdMappingExtractor extractor : extractorList) {
                    extractor.extract(span,
                                      (thisSpan, uTxId) -> {
                                          if (duplication.add(uTxId)) {
                                              mappings.add(new TraceIdMapping(thisSpan.getStartTime() / 1000,
                                                                              uTxId,
                                                                              thisSpan.getTraceId()));

                                          }
                                      });
                }
            }

            return mappings;
        };
    }

    /**
     * see: https://github.com/FrankChen021/bithon/issues/260
     */
    static class CompatibilityIdMappingExtractor implements ITraceIdMappingExtractor {
        static final ITraceIdMappingExtractor INSTANCE = new CompatibilityIdMappingExtractor();

        @Override
        public void extract(TraceSpan span, BiConsumer<TraceSpan, String> callback) {
            if (!"SERVER".equals(span.getKind())) {
                return;
            }
            String upstreamTraceId = span.getTags().get("upstreamTraceId");
            if (StringUtils.hasText(upstreamTraceId)) {
                callback.accept(span, upstreamTraceId);
            }
        }
    }
}
