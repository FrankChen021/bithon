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

package org.bithon.server.collector.source.brpc;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.tracing.BrpcTraceSpanMessage;
import org.bithon.agent.rpc.brpc.tracing.ITraceCollector;
import org.bithon.server.collector.source.http.TraceHttpCollector;
import org.bithon.server.collector.source.otel.OtelHttpTraceCollector;
import org.bithon.server.sink.tracing.ITraceMessageSink;
import org.bithon.server.sink.tracing.source.ITraceMessageSource;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/23 11:19 下午
 */
@Slf4j
@JsonTypeName("local")
public class BrpcTraceCollector implements ITraceCollector, AutoCloseable, ITraceMessageSource {

    private final ApplicationContext applicationContext;
    private ITraceMessageSink processor;
    private BrpcCollectorServer.ServiceGroup serviceGroup;

    @JsonCreator
    public BrpcTraceCollector(@JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void sendTrace(BrpcMessageHeader header,
                          List<BrpcTraceSpanMessage> spans) {
        if (CollectionUtils.isEmpty(spans)) {
            return;
        }

        log.debug("Receiving trace message:{}", spans);
        processor.process("trace",
                          spans.stream()
                               .map(span -> toSpan(header, span))
                               .collect(Collectors.toList()));
    }

    private TraceSpan toSpan(BrpcMessageHeader header,
                             BrpcTraceSpanMessage spanBody) {
        TraceSpan traceSpan = new TraceSpan();
        traceSpan.setAppName(header.getAppName());
        traceSpan.setInstanceName(header.getInstanceName());
        traceSpan.setAppType(header.getAppType().name());
        traceSpan.setKind(spanBody.getKind());
        traceSpan.setName(spanBody.getName());
        traceSpan.setTraceId(spanBody.getTraceId());
        traceSpan.setSpanId(spanBody.getSpanId());
        traceSpan.setParentSpanId(StringUtils.isEmpty(spanBody.getParentSpanId())
                                      ? ""
                                      : spanBody.getParentSpanId());
        traceSpan.setParentApplication(spanBody.getParentAppName());
        traceSpan.setStartTime(spanBody.getStartTime());
        traceSpan.setEndTime(spanBody.getEndTime());
        traceSpan.setCostTime(spanBody.getEndTime() - spanBody.getStartTime());
        traceSpan.setClazz(spanBody.getClazz());
        traceSpan.setMethod(spanBody.getMethod());

        // The returned Map is unmodifiable,
        // here it's turned into a mutable one because the sink process might perform transformation on this Map object.
        // Also, a TreeMap is used to order the keys, which is consistent with the definition in TraceSpan
        traceSpan.setTags(new TreeMap<>(spanBody.getTagsMap()));
        return traceSpan;
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void start() {
        Integer port = this.applicationContext.getBean(BrpcCollectorConfig.class).getPort().get("tracing");
        serviceGroup = this.applicationContext.getBean(BrpcCollectorServer.class)
                                              .addService("trace", ITraceCollector.class, this, port);
    }

    @Override
    public void registerProcessor(ITraceMessageSink processor) {
        this.processor = processor;

        // @ConditionalOnProperty(value = "collector-http.enabled", havingValue = "true")
        this.applicationContext.getBean(TraceHttpCollector.class).setProcessor(processor);

        // @ConditionalOnProperty(value = "collector-otel.enabled", havingValue = "true")
        this.applicationContext.getBean(OtelHttpTraceCollector.class).setProcessor(processor);
    }

    @Override
    public void stop() {
        serviceGroup.stop("trace");
    }
}
