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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bithon.server.common.utils.collection.IteratorableCollection;
import org.bithon.server.tracing.sink.ITraceMessageSink;
import org.bithon.server.tracing.sink.TraceSpan;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    private final ITraceMessageSink traceSink;

    public TraceHttpCollector(ObjectMapper om,
                              ITraceMessageSink traceSink) {
        this.om = om;
        this.traceSink = traceSink;
    }

    @PostMapping("/api/collector/trace")
    public void span(HttpServletRequest request) throws IOException {
        String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        log.trace("receive spans:{}", body);

        final List<TraceSpan> spans = om.readValue(body, new TypeReference<ArrayList<TraceSpan>>() {
        });

        Iterator<TraceSpan> iterator = spans.iterator();
        if ("clickhouse".equals(spans.get(0).appName)) {
            iterator = new ClickHouseAdaptor(iterator);
        }
        this.traceSink.process("trace", IteratorableCollection.of(iterator));
    }

    static class ClickHouseAdaptor implements Iterator<TraceSpan> {
        private final Iterator<TraceSpan> delete;

        ClickHouseAdaptor(Iterator<TraceSpan> delete) {
            this.delete = delete;
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

            //
            // split full qualified method name into clazz and method
            //
            // first change to dot-separated calling style
            String fullQualifiedName = span.getMethod().replaceAll("::", ".");
            if (fullQualifiedName.endsWith("()")) {
                // remove the ending parenthesis
                fullQualifiedName = fullQualifiedName.substring(0, fullQualifiedName.length() - 2);
            }
            int idx = fullQualifiedName.lastIndexOf(' ');
            if (idx >= 0) {
                // discard the return-type
                fullQualifiedName = fullQualifiedName.substring(idx + 1);
            }

            idx = fullQualifiedName.lastIndexOf(".");
            if (idx >= 0) {
                // split the class the method
                span.setClazz(fullQualifiedName.substring(0, idx));
                span.setMethod(fullQualifiedName.substring(idx + 1));
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
