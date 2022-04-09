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

import lombok.extern.slf4j.Slf4j;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.tracing.BrpcTraceSpanMessage;
import org.bithon.agent.rpc.brpc.tracing.ITraceCollector;
import org.bithon.component.commons.collection.IteratorableCollection;
import org.bithon.server.sink.common.service.UriNormalizer;
import org.bithon.server.sink.tracing.ITraceMessageSink;
import org.bithon.server.sink.tracing.TraceSpanHelper;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/23 11:19 下午
 */
@Slf4j
public class BrpcTraceCollector implements ITraceCollector, AutoCloseable {

    private final ITraceMessageSink traceSink;
    private final UriNormalizer uriNormalizer;

    public BrpcTraceCollector(ITraceMessageSink traceSink, UriNormalizer uriNormalizer) {
        this.traceSink = traceSink;
        this.uriNormalizer = uriNormalizer;
    }

    @Override
    public void sendTrace(BrpcMessageHeader header,
                          List<BrpcTraceSpanMessage> spans) {
        if (CollectionUtils.isEmpty(spans)) {
            return;
        }

        log.debug("Receiving trace message:{}", spans);
        traceSink.process("trace", toSpan(header, spans));
    }

    private IteratorableCollection<TraceSpan> toSpan(BrpcMessageHeader header, List<BrpcTraceSpanMessage> messages) {

        Iterator<BrpcTraceSpanMessage> delegate = messages.iterator();
        return IteratorableCollection.of(new Iterator<TraceSpan>() {
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public TraceSpan next() {
                BrpcTraceSpanMessage spanMessage = delegate.next();

                TraceSpan traceSpan = new TraceSpan();
                traceSpan.setAppName(header.getAppName());
                traceSpan.setInstanceName(header.getInstanceName());
                traceSpan.setKind(spanMessage.getKind());
                traceSpan.setName(spanMessage.getName());
                traceSpan.setTraceId(spanMessage.getTraceId());
                traceSpan.setSpanId(spanMessage.getSpanId());
                traceSpan.setParentSpanId(StringUtils.isEmpty(spanMessage.getParentSpanId())
                                          ? ""
                                          : spanMessage.getParentSpanId());
                traceSpan.setParentApplication(spanMessage.getParentAppName());
                traceSpan.setStartTime(spanMessage.getStartTime());
                traceSpan.setEndTime(spanMessage.getEndTime());
                traceSpan.setCostTime(spanMessage.getEndTime() - spanMessage.getStartTime());
                traceSpan.setTags(spanMessage.getTagsMap());
                traceSpan.setClazz(spanMessage.getClazz());
                traceSpan.setMethod(spanMessage.getMethod());

                TraceSpanHelper.flatten(traceSpan, uriNormalizer);

                return traceSpan;
            }
        });
    }

    @Override
    public void close() throws Exception {
        traceSink.close();
    }
}
