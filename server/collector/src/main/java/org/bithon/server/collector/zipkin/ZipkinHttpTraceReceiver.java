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

package org.bithon.server.collector.zipkin;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.UTF8StreamJsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.ReflectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.spring.ThreadNameScope;
import org.bithon.server.pipeline.tracing.ITraceProcessor;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Zipkin V2 API receiver that handles Zipkin spans in JSON format
 *
 * @author frank.chen021@outlook.com
 */
@Slf4j
@RestController
@ConditionalOnProperty(value = "bithon.receivers.traces.zipkin.enabled", havingValue = "true")
public class ZipkinHttpTraceReceiver {

    private final ObjectReader objectReader;
    private static final int MAX_BATCH_SIZE = 1000;

    @Setter
    private ITraceProcessor processor;

    public ZipkinHttpTraceReceiver(ObjectMapper objectMapper) {
        this.objectReader = objectMapper.readerFor(ZipkinSpan.class);
    }

    @ThreadNameScope(template = "^([a-zA-Z-]+)", value = "http-zipkin-")
    @PostMapping({"/api/collector/zipkin/v2/spans"})
    public void receiveZipkinSpans(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (processor == null) {
            return;
        }

        InputStream is = request.getInputStream();

        // Handle compressed data
        String encoding = request.getHeader("Content-Encoding");
        if (!StringUtils.isEmpty(encoding)) {
            switch (encoding) {
                case "gzip":
                    is = new GZIPInputStream(is);
                    break;
                case "deflate":
                    is = new InflaterInputStream(is);
                    break;
                default:
                    String message = StringUtils.format("Not supported Content-Encoding [%s] from remote [%s]", encoding, request.getRemoteAddr());
                    response.getWriter().println(message);
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    return;
            }
        }

        List<TraceSpan> traceSpans = new ArrayList<>(MAX_BATCH_SIZE);

        // Parse JSON using streaming to reduce memory pressure
        try (JsonParser parser = objectReader.createParser(is)) {
            try {
                JsonToken token = parser.nextToken();
                if (token != JsonToken.START_ARRAY) {
                    String message = StringUtils.format("Expecting JSON array from [%s]", request.getRemoteAddr());
                    response.getWriter().println(message);
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    return;
                }

                // Process each span in the JSON array
                while (parser.nextToken() == JsonToken.START_OBJECT) {
                    ZipkinSpan zipkinSpan = objectReader.readValue(parser);

                    traceSpans.add(zipkinSpan.toTraceSpan());

                    // Process in batches to control memory usage
                    if (traceSpans.size() >= MAX_BATCH_SIZE) {
                        processSpans(traceSpans);
                        traceSpans = new ArrayList<>(MAX_BATCH_SIZE);
                    }
                }
            } catch (IOException e) {
                String message = StringUtils.format("Invalid input from [%s]: %s", request.getRemoteAddr(), e.getMessage());
                log.error(message, e);

                // For parse exception, dump the buffer that fails to parse
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

        // Process any remaining spans
        processSpans(traceSpans);
    }

    private void processSpans(List<TraceSpan> traceSpans) {
        if (traceSpans.isEmpty()) {
            return;
        }

        this.processor.process("trace", traceSpans);
    }
}
