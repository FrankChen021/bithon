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

package org.bithon.server.collector.http;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.UTF8StreamJsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.utils.ReflectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.pipeline.tracing.ITraceProcessor;
import org.bithon.server.storage.tracing.TraceSpan;
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
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * @author frank.chen021@outlook.com
 * @date 24/11/21 2:05 pm
 */
@Slf4j
@RestController
@ConditionalOnProperty(value = "bithon.receivers.traces.http.enabled", havingValue = "true")
public class TraceHttpCollector {

    private final ObjectMapper om;
    private final TraceHttpCollectorConfig config;
    private final ThreadPoolExecutor executor;

    @Setter
    private ITraceProcessor processor;

    public TraceHttpCollector(TraceHttpCollectorConfig config,
                              ObjectMapper objectMapper) {
        this.config = config;
        this.om = objectMapper;
        this.executor = new ThreadPoolExecutor(1,
                                               4,
                                               1L,
                                               TimeUnit.MINUTES,
                                               new LinkedBlockingQueue<>(8),
                                               NamedThreadFactory.of("trace-http-processor"),
                                               new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @PostMapping("/api/collector/trace")
    public void span(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (this.processor == null) {
            return;
        }

        InputStream is = request.getInputStream();

        String encoding = request.getHeader("Content-Encoding");
        if (!StringUtils.isEmpty(encoding)) {
            switch (encoding) {
                case "gzip":
                    is = new GZIPInputStream(is);
                    break;
                case "deflate":
                    is = new InflaterInputStream(is);
                    break;
                case "lz4":
                    // Currently only FramedLZ4 is supported
                    is = new FramedLZ4CompressorInputStream(is);
                    break;
                default:
                    String message = StringUtils.format("Not supported Content-Encoding [%s] from remote [%s]", encoding, request.getRemoteAddr());
                    response.getWriter().println(message);
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    return;
            }
        }

        List<TraceSpan> spans = new ArrayList<>(1024);

        // Create parser manually so that this parser can be accessed in the catch handler
        try (JsonParser parser = om.createParser(is)) {
            try {
                JsonToken token = parser.nextToken();
                if (token != JsonToken.START_ARRAY) {
                    return;
                }

                // JSONArray format
                while (parser.nextToken() == JsonToken.START_OBJECT) {
                    spans.add(om.readValue(parser, TraceSpan.class));

                    // This controls the size of content reading from HTTP,
                    // to avoid excessive memory pressure at the collector side
                    if (spans.size() >= config.getMaxRowsPerBatch()) {
                        process(spans);

                        // Create a new array to hold the next batch
                        spans = new ArrayList<>(256);
                    }
                }
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

        process(spans);
    }

    private void process(List<TraceSpan> spans) {
        if (spans.isEmpty()) {
            return;
        }
        this.executor.execute(() -> this.processor.process("trace", spans));
    }
}
