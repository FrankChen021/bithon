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

import org.bithon.server.common.utils.datetime.TimeSpan;
import org.bithon.server.tracing.sink.TraceSpan;
import org.bithon.server.web.service.tracing.service.TraceService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.sql.Timestamp;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 3:53 下午
 */
@CrossOrigin
@RestController
public class TraceApi {

    private final TraceService traceService;

    public TraceApi(TraceService traceService) {
        this.traceService = traceService;
    }

    @PostMapping("/api/trace/getTraceById")
    public GetTraceByIdResponse getTraceById(@Valid @RequestBody GetTraceByIdRequest request) {
        List<TraceSpan> spanList = traceService.getTraceByTraceId(request.getId(),
                                                                  request.getType(),
                                                                  request.getStartTimeISO8601(),
                                                                  request.getEndTimeISO8601(),
                                                                  request.isHierachy());

        return new GetTraceByIdResponse(spanList, traceService.buildMap(spanList));
    }

    @PostMapping("/api/trace/getTraceDistribution")
    public GetTraceDistributionResponse getTraceDistribution(@Valid @RequestBody GetTraceDistributionRequest request) {
        return traceService.getTraceDistribution(request.getApplication(),
                                                 request.getStartTimeISO8601(),
                                                 request.getEndTimeISO8601());
    }

    @PostMapping("/api/trace/getTraceList")
    public GetTraceListResponse getTraceList(@Valid @RequestBody GetTraceListRequest request) {
        Timestamp start = TimeSpan.fromISO8601(request.getStartTimeISO8601()).toTimestamp();
        Timestamp end = TimeSpan.fromISO8601(request.getEndTimeISO8601()).toTimestamp();

        return new GetTraceListResponse(
            traceService.getTraceListSize(request.getApplication(), start, end),
            traceService.getTraceList(request.getApplication(),
                                      start,
                                      end,
                                      request.getOrderBy(),
                                      request.getOrder(),
                                      request.getPageNumber(),
                                      request.getPageSize())
        );
    }

    @PostMapping("/api/trace/getChildSpans")
    public List<TraceSpan> getChildSpans(@Valid @RequestBody GetChildSpansRequest request) {
        return traceService.getTraceByParentSpanId(request.getParentSpanId());
    }
}
