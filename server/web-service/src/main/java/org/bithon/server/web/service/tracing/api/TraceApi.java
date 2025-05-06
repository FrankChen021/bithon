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

package org.bithon.server.web.service.tracing.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.bithon.component.commons.utils.CloseableIterator;
import org.bithon.component.commons.utils.Watch;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.query.Limit;
import org.bithon.server.datasource.query.OrderBy;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.TimeSeriesQueryResult;
import org.bithon.server.web.service.tracing.service.TraceService;
import org.bithon.server.web.service.tracing.service.TraceTopoBuilder;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 3:53 下午
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class TraceApi {

    private final TraceService traceService;
    private final ObjectMapper objectMapper;

    public TraceApi(TraceService traceService, ObjectMapper objectMapper) {
        this.traceService = traceService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/trace/getTraceById")
    public GetTraceByIdResponse getTraceById(@Valid @RequestBody GetTraceByIdRequest request) {
        Watch<List<TraceSpan>> getSpanList = new Watch<>(() -> {
            try (CloseableIterator<TraceSpan> iterator = traceService.getTraceByTraceId(request.getId(),
                                                                                        request.getType(),
                                                                                        request.getStartTimeISO8601(),
                                                                                        request.getEndTimeISO8601())) {
                List<TraceSpan> spans = new ArrayList<>(128);
                while (iterator.hasNext()) {
                    spans.add(iterator.next());
                }
                return spans;
            }
        });

        Watch<List<TraceSpanBo>> transformResult = new Watch<>(() -> traceService.transformSpanList(getSpanList.getResult(), request.isAsTree()));

        Watch<TraceTopo> buildTopo = new Watch<>(() -> new TraceTopoBuilder().build(transformResult.getResult()));

        Map<String, Long> profileEvents = new HashMap<>();
        profileEvents.put("getSpanList", getSpanList.getDuration());
        profileEvents.put("transformation", transformResult.getDuration());
        profileEvents.put("buildTopo", buildTopo.getDuration());

        return new GetTraceByIdResponse(transformResult.getResult(),
                                        buildTopo.getResult(),
                                        profileEvents);
    }

    @PostMapping("/api/trace/getTraceById/v2")
    public ResponseEntity<StreamingResponseBody> getTraceByIdV2(@Valid @RequestBody GetTraceByIdRequest request,
                                                                HttpServletRequest httpRequest) {
        // Check if client accepts gzip encoding
        String acceptEncoding = httpRequest.getHeader(HttpHeaders.ACCEPT_ENCODING);
        boolean useGzip = acceptEncoding != null && acceptEncoding.contains("gzip");

        StreamingResponseBody responseBodyStream = os -> {
            // Create the appropriate output stream based on Accept-Encoding
            try (OutputStream outputStream = useGzip ? new GZIPOutputStream(os) : os;
                 JsonGenerator jsonGenerator = objectMapper.getFactory()
                                                           .createGenerator(outputStream)
                                                           .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

                 CloseableIterator<TraceSpan> iterator = traceService.getTraceByTraceId(request.getId(),
                                                                                        request.getType(),
                                                                                        request.getStartTimeISO8601(),
                                                                                        request.getEndTimeISO8601())) {

                if (iterator.hasNext()) {
                    // write header
                    jsonGenerator.writeStartArray();
                    {
                        jsonGenerator.writeString("traceId");
                        jsonGenerator.writeString("spanId");
                        jsonGenerator.writeString("parentSpanId");
                        jsonGenerator.writeString("appName");
                        jsonGenerator.writeString("instanceName");
                        jsonGenerator.writeString("name");
                        jsonGenerator.writeString("kind");
                        jsonGenerator.writeString("startTimeUs");
                        jsonGenerator.writeString("endTimeUs");
                        jsonGenerator.writeString("costTimeUs");
                        jsonGenerator.writeString("clazz");
                        jsonGenerator.writeString("method");
                        jsonGenerator.writeString("status");
                        jsonGenerator.writeString("normalizedUri");
                        jsonGenerator.writeString("tags");
                    }
                    jsonGenerator.writeEndArray();
                    jsonGenerator.writeRaw('\n'); // Using writeRaw for newline
                }

                while (iterator.hasNext()) {
                    jsonGenerator.writeStartArray(); // Start JSON array for the span
                    {
                        TraceSpan span = iterator.next();

                        jsonGenerator.writeObject(span.getTraceId());
                        jsonGenerator.writeObject(span.getSpanId());
                        jsonGenerator.writeObject(span.getParentSpanId());
                        jsonGenerator.writeObject(span.getAppName());
                        jsonGenerator.writeObject(span.getInstanceName());

                        jsonGenerator.writeObject(span.getName());
                        jsonGenerator.writeObject(span.getKind());

                        jsonGenerator.writeObject(span.getStartTime());
                        jsonGenerator.writeObject(span.getEndTime());
                        jsonGenerator.writeObject(span.getCostTime());

                        jsonGenerator.writeObject(span.getClazz());
                        jsonGenerator.writeObject(span.getMethod());
                        jsonGenerator.writeObject(span.getStatus());
                        jsonGenerator.writeObject(span.getNormalizedUri());
                        jsonGenerator.writeObject(span.getTags());
                    }
                    jsonGenerator.writeEndArray(); // End JSON array for the span
                    jsonGenerator.writeRaw('\n'); // Using writeRaw for newline
                    jsonGenerator.flush();
                }
            } catch (IOException e) {
                // Log the exception if needed
                throw new RuntimeException("Error streaming trace spans", e);
            }
        };

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                                                                   .contentType(MediaType.parseMediaType("application/x-ndjson"));

        // Add Content-Encoding header if using gzip
        if (useGzip) {
            responseBuilder.header(HttpHeaders.CONTENT_ENCODING, "gzip");
        }

        return responseBuilder.body(responseBodyStream);
    }

    /**
     * Deprecated,
     * use {@link org.bithon.server.web.service.datasource.api.IDataSourceApi#timeseriesV4(QueryRequest)} instead
     */
    @Deprecated
    @PostMapping("/api/trace/getTraceDistribution")
    public TimeSeriesQueryResult getTraceDistribution(@Valid @RequestBody GetTraceDistributionRequest request) {
        return traceService.getTraceDistribution(request.getExpression(),
                                                 TimeSpan.fromISO8601(request.getStartTimeISO8601()),
                                                 TimeSpan.fromISO8601(request.getEndTimeISO8601()),
                                                 request.getBucketCount());
    }

    /**
     * use {@link org.bithon.server.web.service.datasource.api.IDataSourceApi#list(QueryRequest)} instead
     */
    @Deprecated
    @PostMapping("/api/trace/getTraceList")
    public GetTraceListResponse getTraceList(@Valid @RequestBody GetTraceListRequest request) {
        Timestamp start = TimeSpan.fromISO8601(request.getStartTimeISO8601()).toTimestamp();
        Timestamp end = TimeSpan.fromISO8601(request.getEndTimeISO8601()).toTimestamp();

        return new GetTraceListResponse(
            request.getPageNumber() == 0 ? traceService.getTraceListSize(request.getExpression(), start, end) : 0,
            request.getPageNumber(),
            traceService.getTraceList(request.getExpression(),
                                      start,
                                      end,
                                      new OrderBy(request.getOrderBy(), request.getOrder()),
                                      new Limit(request.getPageSize(), request.getPageNumber() * request.getPageSize()))
        );
    }

    @PostMapping("/api/trace/getChildSpans")
    public List<TraceSpan> getChildSpans(@Valid @RequestBody GetChildSpansRequest request) {
        return traceService.getTraceByParentSpanId(request.getParentSpanId());
    }
}
