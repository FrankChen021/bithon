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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.sink.common.service.UriNormalizer;
import org.bithon.server.sink.tracing.sink.ITraceMessageSink2;
import org.bithon.server.sink.tracing.transform.TraceSpanTransformer;
import org.bithon.server.storage.datasource.input.transformer.ExceptionSafeTransformer;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:38 AM
 */
@Slf4j
@JsonTypeName("local")
public class LocalTraceSink implements ITraceMessageSink {

    private final List<ITraceMessageSink2> sinks;

    private final List<ITransformer> transformers;

    @JsonCreator
    public LocalTraceSink(@JacksonInject(useInput = OptBoolean.FALSE) TraceSinkConfig traceSinkConfig,
                          @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                          @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        this.transformers = createTransformers(traceSinkConfig.getTransforms(), applicationContext, objectMapper);
        this.sinks = createSinks(traceSinkConfig.getSinks(), objectMapper);

    }

    private List<ITransformer> createTransformers(List<Map<String, String>> transforms,
                                                  ApplicationContext applicationContext,
                                                  ObjectMapper objectMapper) {
        List<ITransformer> transformers = new ArrayList<>();
        transformers.add(new TraceSpanTransformer(applicationContext.getBean(UriNormalizer.class)));

        if (CollectionUtils.isEmpty(transforms)) {
            return transformers;
        }

        for (Map<String, String> transform : transforms) {
            try {
                transformers.add(new ExceptionSafeTransformer(objectMapper.readValue(objectMapper.writeValueAsBytes(transform),
                                                                                     ITransformer.class)));
            } catch (IOException ignored) {
            }
        }
        return transformers;
    }

    private List<ITraceMessageSink2> createSinks(List<Map<String, String>> sinks,
                                                 ObjectMapper objectMapper) {
        List<ITraceMessageSink2> sinkObjects = new ArrayList<>();
        for (Map<String, String> sink : sinks) {
            try {
                sinkObjects.add(objectMapper.readValue(objectMapper.writeValueAsBytes(sink),
                                                       ITraceMessageSink2.class));
            } catch (IOException ignored) {
            }
        }
        return sinkObjects;
    }

    public <T extends ITraceMessageSink2> T link(T sink) {
        synchronized (sinks) {
            sinks.add(sink);
        }
        return sink;
    }

    public <T extends ITraceMessageSink2> T unlink(T sink) {
        synchronized (sinks) {
            sinks.remove(sink);
        }
        return sink;
    }

    @Override
    public void process(String messageType, List<TraceSpan> spans) {
        if (CollectionUtils.isEmpty(spans)) {
            return;
        }

        Iterator<TraceSpan> iterator = spans.iterator();
        while (iterator.hasNext()) {
            TraceSpan span = iterator.next();

            for (ITransformer transformer : transformers) {
                if (!transformer.transform(span)) {
                    iterator.remove();
                }
            }
        }

        if (spans.isEmpty()) {
            return;
        }

        ITraceMessageSink2[] sinks = this.sinks.toArray(new ITraceMessageSink2[0]);
        for (ITraceMessageSink2 sink : sinks) {
            try {
                sink.process(messageType, spans);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() throws Exception {
        for (ITraceMessageSink2 sink : sinks) {
            sink.close();
        }
    }
}
