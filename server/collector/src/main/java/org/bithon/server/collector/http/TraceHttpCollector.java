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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.UTF8StreamJsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.utils.CollectionUtils;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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

    private final ObjectReader objectReader;
    private final TraceHttpCollectorConfig config;
    private final ThreadPoolExecutor executor;
    private final ObjectMapper objectMapper;

    @Setter
    private ITraceProcessor processor;

    public TraceHttpCollector(TraceHttpCollectorConfig config,
                              ObjectMapper objectMapper) {
        this.config = config;
        this.executor = new ThreadPoolExecutor(1,
                                               4,
                                               1L,
                                               TimeUnit.MINUTES,
                                               new LinkedBlockingQueue<>(8),
                                               NamedThreadFactory.nonDaemonThreadFactory("trace-http-processor"),
                                               new ThreadPoolExecutor.CallerRunsPolicy());

        this.objectReader = objectMapper.readerFor(TraceSpan.class).with(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION);
        this.objectMapper = objectMapper;
    }

    /**
     * Because we provide HTTP endpoint for different protocols(jaeger, zipkin, otlp, etc.),
     * it's better to customize the thread name by {@link ThreadNameScope} for each protocol to improve observability.
     */
    @ThreadNameScope(template = "^([a-zA-Z-]+)", value = "http-bithon-")
    @PostMapping("/api/collector/trace")
    public void receiveSpans(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

        // Create a batching consumer that collects spans and processes them in batches
        Consumer<List<TraceSpan>> batchConsumer = batch -> this.executor.execute(() -> this.processor.process("trace", batch));

        ParseResult result = new BatchParser(this.objectReader,
                                             this.config.getMaxRowsPerBatch()).parse(is, batchConsumer);

        response.setStatus(result.getException() != null ? HttpStatus.INTERNAL_SERVER_ERROR.value() : HttpStatus.OK.value());
        response.setContentType("application/json");
        try {
            response.getOutputStream().write(this.objectMapper.writeValueAsBytes(result));
        } catch (IOException e) {
            log.warn("Error to flush result to client: {}", e.getMessage());
        }
    }

    static class BatchParser {
        private final TraceSpanParser parser;
        private final int batchSize;

        public BatchParser(ObjectReader objectReader, int batchSize) {
            this.parser = new TraceSpanParser(objectReader);
            this.batchSize = batchSize;
        }

        public ParseResult parse(InputStream inputStream,
                                 Consumer<List<TraceSpan>> batchConsumer) {
            List<TraceSpan> batch = new ArrayList<>(batchSize);

            Consumer<TraceSpan> spanCollector = span -> {
                batch.add(span);
                if (batch.size() >= batchSize) {
                    batchConsumer.accept(new ArrayList<>(batch));
                    batch.clear();
                }
            };

            try {
                return parser.parse(inputStream, spanCollector);
            } finally {
                if (!batch.isEmpty()) {
                    // Process any remaining spans in the batch
                    batchConsumer.accept(new ArrayList<>(batch));
                }
            }
        }
    }

    @Getter
    public static class ParseResult {
        @Getter(AccessLevel.NONE)
        private final boolean errors;

        @JsonIgnore
        private final Exception exception;

        private final int successfulSpans;
        private final int errorSpans;
        private final String message;
        private final Map<String, ErrorInfo> parseErrors;

        public boolean hasErrors() {
            return errors;
        }

        private ParseResult(boolean errors, int successfulSpans, String message, Exception exception, Map<String, ErrorInfo> parseErrors) {
            this.errors = errors;
            this.message = message;
            this.exception = exception;
            this.parseErrors = parseErrors != null ? parseErrors : Collections.emptyMap();
            this.successfulSpans = successfulSpans;
            this.errorSpans = this.parseErrors.values().stream()
                                              .mapToInt(ErrorInfo::getErrorCount)
                                              .sum();
        }

        public static ParseResult success(int processedSpans, Map<String, ErrorInfo> errors) {
            return new ParseResult(CollectionUtils.isNotEmpty(errors), processedSpans, null, null, errors);
        }

        public static ParseResult failure(int processedSpans, String errorMessage, Exception exception, Map<String, ErrorInfo> errors) {
            return new ParseResult(true,
                                   processedSpans,
                                   errorMessage,
                                   exception,
                                   errors);
        }
    }

    @Getter
    public static class ErrorInfo {
        private final String errorType;
        private int errorCount;
        private TraceSpan lastErrorSpan;

        public ErrorInfo(String errorType) {
            this.errorType = errorType;
        }

        public void addErrorSpan(TraceSpan errorSpan) {
            this.lastErrorSpan = errorSpan;
            this.errorCount++;
        }
    }

    static class TraceSpanParser {
        private final ObjectReader objectReader;
        private final Map<String, ErrorInfo> errors;
        private int processedSpans;

        public TraceSpanParser(ObjectReader objectReader) {
            this.objectReader = objectReader;
            this.errors = new HashMap<>();
            this.processedSpans = 0;
        }

        public ParseResult parse(InputStream inputStream,
                                 Consumer<TraceSpan> spanConsumer) {
            // Clear errors and reset counter from previous parsing
            this.errors.clear();
            this.processedSpans = 0;

            // Create parser manually so that this parser can be accessed in the catch handler
            try (JsonParser parser = objectReader.createParser(inputStream)) {
                try {
                    JsonToken token = parser.nextToken();
                    if (token == JsonToken.START_ARRAY) {
                        // JSONArray format
                        while (parser.nextToken() == JsonToken.START_OBJECT) {
                            TraceSpan traceSpan = normalizeTraceSpan(objectReader.readValue(parser));
                            if (traceSpan != null) {
                                spanConsumer.accept(traceSpan);
                            }
                        }
                    } else if (token == JsonToken.START_OBJECT) {
                        // List of JSON objects
                        do {
                            TraceSpan traceSpan = normalizeTraceSpan(objectReader.readValue(parser));
                            if (traceSpan != null) {
                                spanConsumer.accept(traceSpan);
                            }
                        } while (parser.nextToken() == JsonToken.START_OBJECT);
                    }
                } catch (IOException e) {
                    //
                    // for parse exception, dump the buffer that fails to parse
                    //
                    if (((e instanceof JsonParseException) || (e.getCause() instanceof JsonParseException))
                        && parser instanceof UTF8StreamJsonParser) {
                        Object buffer = ReflectionUtils.getFieldValue(parser, "_inputBuffer");
                        Object end = ReflectionUtils.getFieldValue(parser, "_inputEnd");
                        if (buffer != null && end != null) {
                            try {
                                log.error("Failed to parse JSON. Input content is:\n{}", new String((byte[]) buffer, 0, (int) end, StandardCharsets.UTF_8));
                            } catch (RuntimeException ignored) {
                            }
                        }
                    }

                    return ParseResult.failure(this.processedSpans, e.getMessage(), e, new HashMap<>(this.errors));
                }
            } catch (IOException e) {
                return ParseResult.failure(this.processedSpans, "Failed to create JSON parser: " + e.getMessage(), e, new HashMap<>(this.errors));
            }

            return ParseResult.success(this.processedSpans, new HashMap<>(this.errors));
        }

        private TraceSpan normalizeTraceSpan(TraceSpan span) {
            // Validate startTime (should be positive and reasonable - in microseconds)
            if (!isValidTime(span.startTime)) {
                trackError("INVALID_START_TIME", span);
                return null;
            }

            // Validate traceId (must be non-empty)
            if (!isValidTraceId(span.traceId)) {
                trackError("INVALID_TRACE_ID", span);
                return null;
            }

            // Validate spanId (must be non-empty)
            if (!isValidSpanId(span.spanId)) {
                trackError("INVALID_SPAN_ID", span);
                return null;
            }

            // Validate kind (must be valid SpanKind)
            if (!isValidKind(span.kind)) {
                trackError("INVALID_KIND", span);
                return null;
            }

            // Validate endTime (if exists, should be valid like startTime)
            if (span.endTime != 0 && !isValidTime(span.endTime)) {
                trackError("INVALID_END_TIME", span);
                return null;
            }

            // Process valid span
            if (span.method == null) {
                span.method = "";
            }
            if (span.clazz == null) {
                span.clazz = "";
            }
            if (span.endTime == 0) {
                span.endTime = span.startTime + span.costTime;
            }

            this.processedSpans++;

            return span;
        }

        private boolean isValidTime(long time) {
            // Time should be positive and reasonable (in microseconds)
            // Valid range: 1970-01-01 to ~2100 (reasonable timestamp range)
            // 0 (1970-01-01) to ~4,000,000,000,000,000 microseconds (roughly 2096)
            return time > 0 && time < 4_000_000_000_000_000L;
        }

        private boolean isValidTraceId(String traceId) {
            // traceId must be non-null and non-empty
            return traceId != null && !traceId.trim().isEmpty();
        }

        private boolean isValidSpanId(String spanId) {
            // spanId must be non-null and non-empty
            return spanId != null && !spanId.trim().isEmpty();
        }

        private boolean isValidKind(String kind) {
            // kind must be a valid SpanKind enum value (case-insensitive)
            if (kind == null || kind.trim().isEmpty()) {
                return false;
            }

            try {
                // Use SpanKind enum to validate
                SpanKind.valueOf(kind.trim().toUpperCase(java.util.Locale.ENGLISH));
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        private void trackError(String errorType, TraceSpan span) {
            this.errors.computeIfAbsent(errorType, ErrorInfo::new)
                       .addErrorSpan(span);
        }
    }
}
