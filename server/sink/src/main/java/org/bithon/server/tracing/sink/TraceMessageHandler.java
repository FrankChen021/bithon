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

package org.bithon.server.tracing.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.common.handler.AbstractThreadPoolMessageHandler;
import org.bithon.server.common.utils.collection.CloseableIterator;
import org.bithon.server.tracing.TraceConfig;
import org.bithon.server.tracing.mapping.ITraceMappingExtractor;
import org.bithon.server.tracing.mapping.TraceMapping;
import org.bithon.server.tracing.mapping.TraceMappingConfig;
import org.bithon.server.tracing.storage.ITraceStorage;
import org.bithon.server.tracing.storage.ITraceWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:21 下午
 */
@Slf4j
public class TraceMessageHandler extends AbstractThreadPoolMessageHandler<CloseableIterator<TraceSpan>> {

    private final ITraceWriter traceWriter;
    private final List<ITraceMappingExtractor> extractorList;

    public TraceMessageHandler(ApplicationContext applicationContext) {
        super("trace", 2, 10, Duration.ofMinutes(1), 2048);
        this.traceWriter = applicationContext.getBean(ITraceStorage.class).createWriter();

        //
        // instantiate mapping extractors
        //
        TraceConfig config = applicationContext.getBean(TraceConfig.class);
        if (CollectionUtils.isEmpty(config.getMapping())) {
            extractorList = Collections.emptyList();
            return;
        }

        extractorList = new ArrayList<>();
        ObjectMapper mapper = applicationContext.getBean(ObjectMapper.class);
        for (TraceMappingConfig mappingConfig : config.getMapping()) {
            try {
                String json = mapper.writeValueAsString(mappingConfig);
                extractorList.add(mapper.readValue(json, ITraceMappingExtractor.class));
            } catch (IOException e) {
                log.error("Unable to create extractor for type " + mappingConfig.getType(), e);
            }
        }
    }

    @Override
    protected void onMessage(CloseableIterator<TraceSpan> traceSpans) throws IOException {

        //
        // extract mappings
        //
        List<TraceSpan> spanList = new ArrayList<>();
        List<TraceMapping> mappingList = new ArrayList<>();
        Set<String> userTxIds = new HashSet<>();
        while (traceSpans.hasNext()) {
            TraceSpan span = traceSpans.next();
            spanList.add(span);

            if (CollectionUtils.isEmpty(span.getTags())) {
                continue;
            }

            for (ITraceMappingExtractor extractor : this.extractorList) {
                List<TraceMapping> mappings = extractor.extract(span);

                // remove duplication
                for (TraceMapping mapping : mappings) {
                    if (userTxIds.add(mapping.getUserId())) {
                        mappingList.add(mapping);
                    }
                }
            }
        }
        if (!spanList.isEmpty()) {
            traceWriter.writeSpans(spanList);
            traceWriter.writeMappings(mappingList);
        }
    }

    @Override
    public String getType() {
        return "trace";
    }
}
