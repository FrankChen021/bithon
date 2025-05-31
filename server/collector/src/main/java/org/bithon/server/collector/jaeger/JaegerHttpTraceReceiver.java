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

package org.bithon.server.collector.jaeger;

import io.jaegertracing.thriftjava.Batch;
import io.jaegertracing.thriftjava.Span;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
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
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Jaeger HTTP trace receiver that handles Jaeger Thrift binary data over HTTP.
 * This receiver supports the standard Jaeger collector HTTP endpoint.
 *
 * @author frank.chen021@outlook.com
 */
@Slf4j
@RestController
@ConditionalOnProperty(value = "bithon.receivers.traces.jaeger-http.enabled", havingValue = "true")
public class JaegerHttpTraceReceiver {

    private static final int MAX_BATCH_SIZE = 1000;
    private static final String THRIFT_BINARY_CONTENT_TYPE = "application/vnd.apache.thrift.binary";
    private static final String THRIFT_BINARY_CONTENT_TYPE_2 = "application/x-thrift";
    private static final String THRIFT_COMPACT_CONTENT_TYPE = "application/vnd.apache.thrift.compact";

    @Setter
    private ITraceProcessor processor;

    /**
     * Standard Jaeger collector HTTP endpoint for receiving traces.
     * This endpoint is compatible with Jaeger clients configured to send traces over HTTP.
     */
    @ThreadNameScope(template = "^([a-zA-Z-]+)", value = "http-jaeger-")
    @PostMapping({"/api/collector/jaeger/traces"})
    public void receiveJaegerTraces(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (processor == null) {
            log.warn("No trace processor registered, ignoring traces from {}", request.getRemoteAddr());
            return;
        }

        // Validate content type
        String contentType = request.getContentType();
        if (contentType == null) {
            String message = StringUtils.format("Missing Content-Type header from remote [%s]", request.getRemoteAddr());
            response.getWriter().println(message);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }

        // Determine Thrift protocol based on content type
        boolean useCompactProtocol;
        if (contentType.startsWith(THRIFT_BINARY_CONTENT_TYPE) || contentType.startsWith(THRIFT_BINARY_CONTENT_TYPE_2)) {
            useCompactProtocol = false;
        } else if (contentType.startsWith(THRIFT_COMPACT_CONTENT_TYPE)) {
            useCompactProtocol = true;
        } else {
            String message = StringUtils.format("Unsupported Content-Type [%s] from remote [%s]. Expected [%s] or [%s]",
                                                contentType,
                                                request.getRemoteAddr(),
                                                THRIFT_BINARY_CONTENT_TYPE,
                                                THRIFT_BINARY_CONTENT_TYPE_2,
                                                THRIFT_COMPACT_CONTENT_TYPE);
            response.getWriter().println(message);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }

        InputStream inputStream = request.getInputStream();

        // Handle compressed data
        String encoding = request.getHeader("Content-Encoding");
        if (!StringUtils.isEmpty(encoding)) {
            switch (encoding) {
                case "gzip":
                    inputStream = new GZIPInputStream(inputStream);
                    break;
                case "deflate":
                    inputStream = new InflaterInputStream(inputStream);
                    break;
                default:
                    String message = StringUtils.format("Unsupported Content-Encoding [%s] from remote [%s]",
                                                        encoding, request.getRemoteAddr());
                    response.getWriter().println(message);
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    return;
            }
        }

        try {
            // Process the Thrift data directly from stream
            processThriftData(inputStream, useCompactProtocol, request.getRemoteAddr());

            // Send success response
            response.setStatus(HttpStatus.OK.value());

        } catch (TException e) {
            String message = StringUtils.format("Failed to deserialize Thrift data from remote [%s]: %s",
                                                request.getRemoteAddr(), e.getMessage());
            log.error(message, e);
            response.getWriter().println(message);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        } catch (Exception e) {
            String message = StringUtils.format("Error processing traces from remote [%s]: %s",
                                                request.getRemoteAddr(), e.getMessage());
            log.error(message, e);
            response.getWriter().println(message);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private void processThriftData(InputStream inputStream, boolean useCompactProtocol, String remoteAddr) throws TException {
        try {
            Batch batch = new Batch();
            if (useCompactProtocol) {
                batch.read(new TCompactProtocol(new TIOStreamTransport(inputStream)));
            } else {
                batch.read(new TBinaryProtocol(new TIOStreamTransport(inputStream)));
            }

            ApplicationInstance instance = ApplicationInstance.from(batch);

            // Convert Jaeger spans to TraceSpan objects
            List<TraceSpan> traceSpans = new ArrayList<>();
            if (batch.getSpans() != null) {
                for (Span jaegerSpan : batch.getSpans()) {
                    TraceSpan span = JaegerSpanConverter.convert(instance, jaegerSpan);
                    traceSpans.add(span);

                    // Process in batches to control memory usage
                    if (traceSpans.size() >= MAX_BATCH_SIZE) {
                        processSpans(traceSpans);
                        traceSpans = new ArrayList<>();
                    }
                }
            }

            // Process any remaining spans
            processSpans(traceSpans);
        } catch (TException e) {
            log.error("Failed to deserialize Jaeger batch from {}: {}", remoteAddr, e.getMessage());
            throw e;
        }
    }

    private void processSpans(List<TraceSpan> traceSpans) {
        if (traceSpans.isEmpty()) {
            return;
        }

        try {
            this.processor.process("trace", traceSpans);
        } catch (Exception e) {
            log.error("Error processing {} spans: {}", traceSpans.size(), e.getMessage(), e);
        }
    }
}
