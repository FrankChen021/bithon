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

package org.bithon.server.tracing.handler;

import lombok.Data;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.tracing.BrpcTraceSpanMessage;
import org.bithon.agent.rpc.thrift.service.MessageHeader;
import org.bithon.agent.rpc.thrift.service.trace.TraceSpanMessage;
import org.bithon.server.common.utils.collection.CloseableIterator;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:28 下午
 */
@Data
public class TraceSpan {
    public String appName;
    public String instanceName;
    public String traceId;
    public String spanId;
    public String kind;
    public String parentSpanId;
    public String parentApplication;
    public Map<String, String> tags;
    public long costTime;
    public long startTime;
    public long endTime;
    public String name;
    public String clazz;
    public String method;

    public static CloseableIterator<TraceSpan> of(BrpcMessageHeader header, List<BrpcTraceSpanMessage> messages) {

        Iterator<BrpcTraceSpanMessage> delegate = messages.iterator();
        return new CloseableIterator<TraceSpan>() {
            @Override
            public void close() {
            }

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public TraceSpan next() {
                BrpcTraceSpanMessage spanMessage = delegate.next();

                TraceSpan traceSpan = new TraceSpan();
                traceSpan.appName = header.getAppName();
                traceSpan.instanceName = header.getInstanceName();
                traceSpan.kind = spanMessage.getKind();
                traceSpan.name = spanMessage.getName();
                traceSpan.traceId = spanMessage.getTraceId();
                traceSpan.spanId = spanMessage.getSpanId();
                traceSpan.parentSpanId = StringUtils.isEmpty(spanMessage.getParentSpanId()) ? "" : spanMessage.getParentSpanId();
                traceSpan.parentApplication = spanMessage.getParentAppName();
                traceSpan.startTime = spanMessage.getStartTime();
                traceSpan.endTime = spanMessage.getEndTime();
                traceSpan.costTime = spanMessage.getEndTime() - spanMessage.getStartTime();
                traceSpan.tags = spanMessage.getTagsMap();
                traceSpan.clazz = spanMessage.getClazz();
                traceSpan.method = spanMessage.getMethod();

                return traceSpan;
            }
        };
    }

    public static CloseableIterator<TraceSpan> from(MessageHeader header, List<TraceSpanMessage> messages) {

        Iterator<TraceSpanMessage> delegate = messages.iterator();
        return new CloseableIterator<TraceSpan>() {
            @Override
            public void close() {
            }

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public TraceSpan next() {
                TraceSpanMessage spanMessage = delegate.next();

                TraceSpan traceSpan = new TraceSpan();
                traceSpan.appName = header.appName;
                traceSpan.instanceName = header.instanceName;
                traceSpan.kind = spanMessage.kind;
                traceSpan.name = spanMessage.name;
                traceSpan.traceId = spanMessage.traceId;
                traceSpan.spanId = spanMessage.spanId;
                traceSpan.parentSpanId = spanMessage.parentSpanId == null ? "" : spanMessage.parentSpanId;
                traceSpan.parentApplication = spanMessage.parentAppName;
                traceSpan.startTime = spanMessage.startTime;
                traceSpan.endTime = spanMessage.endTime;
                traceSpan.costTime = spanMessage.endTime - spanMessage.startTime;
                traceSpan.tags = spanMessage.tags;
                traceSpan.clazz = spanMessage.clazz;
                traceSpan.method = spanMessage.method;

                return traceSpan;
            }
        };
    }
}
