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

package org.bithon.server.collector.source.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bithon.server.collector.sink.IMessageSink;
import org.bithon.server.common.utils.collection.CloseableIterator;
import org.bithon.server.tracing.handler.TraceSpan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Frank Chen
 * @date 24/11/21 2:05 pm
 */
@Slf4j
@RestController
public class TraceHttpCollector {

    private final ObjectMapper om;
    private final IMessageSink<CloseableIterator<TraceSpan>> traceSink;

    public TraceHttpCollector(ObjectMapper om,
                              @Qualifier("traceSink") IMessageSink<CloseableIterator<TraceSpan>> traceSink) {
        this.om = om;
        this.traceSink = traceSink;
    }

    @PostMapping("/api/collector/trace")
    public void span(HttpServletRequest request) throws IOException {
        String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        log.trace("receive spans:{}", body);

        final TraceSpan[] spans = om.readValue(body, TraceSpan[].class);
        if (spans.length == 0) {
            return;
        }
        CloseableIterator<TraceSpan> iterator = new CloseableIterator<TraceSpan>() {
            int index = 0;

            @Override
            public void close() {
            }

            @Override
            public boolean hasNext() {
                return index < spans.length;
            }

            @Override
            public TraceSpan next() {
                return spans[index++];
            }
        };
        if ("clickhouse".equals(spans[0].appName)) {
            iterator = new ClickHouseAdaptor(iterator);
        }
        this.traceSink.process("trace", iterator);
    }

    static class ClickHouseAdaptor implements CloseableIterator<TraceSpan> {
        private final CloseableIterator<TraceSpan> delete;

        ClickHouseAdaptor(CloseableIterator<TraceSpan> delete) {
            this.delete = delete;
        }

        @Override
        public void close() throws IOException {
            delete.close();
        }

        @Override
        public boolean hasNext() {
            return delete.hasNext();
        }

        @Override
        public TraceSpan next() {
            TraceSpan span = delete.next();

            // update parentSpanId
            if ("00".equals(span.getParentSpanId())) {
                span.setParentSpanId("");
                span.setKind("SERVER");
            }

            // split full qualified method name into clazz and method
            String fullQualifiedName = span.getMethod();
            if (fullQualifiedName.endsWith("()")) {
                // remove the ending parenthesis
                fullQualifiedName = fullQualifiedName.substring(0, fullQualifiedName.length() - 2);
            }
            int idx = span.getMethod().lastIndexOf("::");
            if (idx >= 0) {
                span.setClazz(fullQualifiedName.substring(0, idx));
                span.setMethod(fullQualifiedName.substring(idx + 2));
            }

            // tidy tags
            Map<String, String> tags = new TreeMap<>();
            for (Map.Entry<String, String> entry : span.getTags().entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();

                int dotIndex = key.lastIndexOf('.');
                if (dotIndex >= 0) {
                    key = key.substring(dotIndex + 1);
                }
                tags.put(key, val);
            }
            span.setTags(tags);

            return span;
        }
    }
}
