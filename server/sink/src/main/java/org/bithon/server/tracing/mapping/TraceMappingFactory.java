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

package org.bithon.server.tracing.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.tracing.TraceConfig;
import org.bithon.server.tracing.sink.TraceSpan;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Frank Chen
 * @date 10/12/21 4:44 PM
 */
@Slf4j
public class TraceMappingFactory {

    public static Function<Collection<TraceSpan>, List<TraceMapping>> create(ApplicationContext context) {
        TraceConfig config = context.getBean(TraceConfig.class);
        if (CollectionUtils.isEmpty(config.getMapping())) {
            return traceSpan -> Collections.emptyList();
        }

        final List<ITraceMappingExtractor> extractorList = new ArrayList<>();
        ObjectMapper mapper = context.getBean(ObjectMapper.class);
        for (TraceMappingConfig mappingConfig : config.getMapping()) {
            try {
                Map<String, Object> map = new HashMap<>();
                map.put("type", mappingConfig.getType());
                map.putAll(mappingConfig.getArgs());
                String json = mapper.writeValueAsString(map);
                extractorList.add(mapper.readValue(json, ITraceMappingExtractor.class));
            } catch (IOException e) {
                log.error("Unable to create extractor for type " + mappingConfig.getType(), e);
            }
        }

        return create(extractorList);
    }

    static Function<Collection<TraceSpan>, List<TraceMapping>> create(ITraceMappingExtractor extractorList) {
        return create(Collections.singletonList(extractorList));
    }

    static Function<Collection<TraceSpan>, List<TraceMapping>> create(List<ITraceMappingExtractor> extractorList) {
        return spanList -> {
            Set<String> duplication = new HashSet<>();

            List<TraceMapping> mappings = new ArrayList<>();
            for (ITraceMappingExtractor extractor : extractorList) {
                for (TraceSpan span : spanList) {
                    if (CollectionUtils.isEmpty(span.getTags())) {
                        continue;
                    }

                    extractor.extract(span,
                                      (thisSpan, uTxId) -> {
                                          if (duplication.add(uTxId)) {
                                              mappings.add(new TraceMapping(thisSpan.getStartTime() / 1000,
                                                                            uTxId,
                                                                            thisSpan.getTraceId()));

                                          }
                                      });
                }
            }

            return mappings;
        };
    }
}
