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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.UTF8StreamJsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.collection.IteratorableCollection;
import org.bithon.component.commons.utils.ReflectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.sink.common.service.UriNormalizer;
import org.bithon.server.sink.tracing.ITraceMessageSink;
import org.bithon.server.sink.tracing.TraceSpanHelper;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * @author frank.chen021@outlook.com
 * @date 24/11/21 2:05 pm
 */
@Slf4j
@RestController
@ConditionalOnProperty(value = "collector-http.enabled", havingValue = "true")
public class TraceHttpCollector {

    private final ObjectMapper om;
    private final ITraceMessageSink traceSink;
    private final UriNormalizer uriNormalizer;

    private final TraceHttpCollectorConfig collectorConfig;

    public TraceHttpCollector(ObjectMapper om,
                              @Qualifier("trace-sink-collector") ITraceMessageSink traceSink,
                              UriNormalizer uriNormalizer,
                              TraceHttpCollectorConfig collectorConfig) {
        this.om = om;
        this.traceSink = traceSink;
        this.uriNormalizer = uriNormalizer;
        this.collectorConfig = collectorConfig;
    }

    @PostMapping("/api/collector/trace")
    public void span(HttpServletRequest request, HttpServletResponse response) throws IOException {
        InputStream is = request.getInputStream();

        String encoding = request.getHeader("Content-Encoding");
        if (!StringUtils.isEmpty(encoding)) {
            if ("gzip".equals(encoding)) {
                is = new GZIPInputStream(is);
            } else if ("deflate".equals(encoding)) {
                is = new InflaterInputStream(is);
            } else {
                String message = StringUtils.format("Not supported Content-Encoding [%s] from remote [%s]", encoding, request.getRemoteAddr());
                response.getWriter().println(message);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return;
            }
        }

        List<TraceSpan> spans;

        // create parser manually so that this parser can be accessed in the catch handler
        try (JsonParser parser = om.createParser(is)) {
            try {
                spans = om.readValue(parser, new TypeReference<ArrayList<TraceSpan>>() {
                });
            } catch (IOException e) {
                String message = StringUtils.format("Invalid input from [%s]: %s", request.getRemoteAddr(), e.getMessage());
                log.error(message, e);

                //
                // for parse exception, dump the buffer that fails to parse
                //
                if (e.getCause() instanceof JsonParseException && parser instanceof UTF8StreamJsonParser) {
                    Object buffer = ReflectionUtils.getFieldValue(parser, "_inputBuffer");
                    Object end = ReflectionUtils.getFieldValue(parser, "_inputEnd");
                    if (buffer != null && end != null) {
                        try {
                            log.error("{}: Input content is:\n{}", message, new String((byte[]) buffer, 0, (int) end, StandardCharsets.UTF_8));
                        } catch (RuntimeException ignored) {
                        }
                    }
                }

                response.getWriter().println(message);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return;
            }
        }
        if (spans.isEmpty()) {
            return;
        }

        String applicationName = spans.get(0).getAppName();

        Iterator<TraceSpan> iterator;
        if (this.collectorConfig.getClickHouseApplications().contains(applicationName)) {
            iterator = new ClickHouseAdaptor(spans.iterator());
        } else {
            iterator = new Iterator<TraceSpan>() {
                private final Iterator<TraceSpan> delegate = spans.iterator();

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public TraceSpan next() {
                    TraceSpan span = delegate.next();
                    TraceSpanHelper.flatten(span, uriNormalizer);
                    return span;
                }
            };
        }
        this.traceSink.process("trace", IteratorableCollection.of(iterator).toCollection());
    }

    class ClickHouseAdaptor implements Iterator<TraceSpan> {
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

            // discard the input parameters of the method
            int parameterStart = span.getMethod().indexOf('(');
            if (parameterStart > 0) {
                span.setMethod(span.getMethod().substring(0, parameterStart));
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
            Map<String, String> tags = new HashMap<>();
            for (Map.Entry<String, String> entry : span.getTags().entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();

                int dotIndex = key.lastIndexOf('.');
                if (dotIndex >= 0) {
                    key = key.substring(dotIndex + 1);
                }
                tags.put(key, val);
            }

            if ("00".equals(span.getParentSpanId()) || "".equals(span.getParentSpanId())) {
                span.setParentSpanId("");
                span.setKind("SERVER");
            } else if ("HTTPHandler".equals(span.getClazz()) && "handleRequest".equals(span.getMethod())) {
                span.setKind("SERVER");
            } else if ("TCPHandler".equals(span.getMethod())) {
                span.setKind("SERVER");
            }

            //
            // standardize tag names
            //
            String sql = tags.remove("statement");
            if (sql != null) {
                tags.put("sql", sql);
            }

            String httpStatus = tags.remove("http_status");
            if (httpStatus != null) {
                tags.put("http.status", httpStatus);
            }

            String uri = tags.remove("uri");
            if (uri != null) {
                tags.put("http.uri", uri);
            }

            span.getTags().clear();
            span.setTags(tags);
            TraceSpanHelper.flatten(span, uriNormalizer);

            return span;
        }
    }
}
