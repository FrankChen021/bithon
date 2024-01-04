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

import org.bithon.component.commons.utils.Watch;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.datasource.api.TimeSeriesQueryResult;
import org.bithon.server.web.service.tracing.service.TraceService;
import org.bithon.server.web.service.tracing.service.TraceTopoBuilder;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 3:53 下午
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class TraceApi {

    private final TraceService traceService;

    public TraceApi(TraceService traceService) {
        this.traceService = traceService;
    }

    @PostMapping("/api/trace/getTraceById")
    public GetTraceByIdResponse getTraceById(@Valid @RequestBody GetTraceByIdRequest request) {
        Watch<List<TraceSpan>> getSpanList = new Watch<>(() -> traceService.getTraceByTraceId(request.getId(),
                                                                                              request.getType(),
                                                                                              request.getStartTimeISO8601(),
                                                                                              request.getEndTimeISO8601()));

        Watch<List<TraceSpan>> buildTree = new Watch<>(() -> traceService.asTree(getSpanList.getResult()));

        Watch<TraceTopo> buildTopo = new Watch<>(() -> new TraceTopoBuilder().build(buildTree.getResult()));

        Map<String, Long> profileEvents = new HashMap<>();
        profileEvents.put("getSpanList", getSpanList.getDuration());
        profileEvents.put("buildTree", buildTree.getDuration());
        profileEvents.put("buildTopo", buildTopo.getDuration());

        return new GetTraceByIdResponse(request.isAsTree() ? buildTree.getResult() : getSpanList.getResult(),
                                        buildTopo.getResult(),
                                        profileEvents);
    }

    @PostMapping("/api/trace/getTraceDistribution")
    public TimeSeriesQueryResult getTraceDistribution(@Valid @RequestBody GetTraceDistributionRequest request) {
        return traceService.getTraceDistribution(request.getFilters(),
                                                 request.getExpression(),
                                                 TimeSpan.fromISO8601(request.getStartTimeISO8601()),
                                                 TimeSpan.fromISO8601(request.getEndTimeISO8601()),
                                                 request.getBucketCount());
    }

    @PostMapping("/api/trace/getTraceList")
    public GetTraceListResponse getTraceList(@Valid @RequestBody GetTraceListRequest request) {
        Timestamp start = TimeSpan.fromISO8601(request.getStartTimeISO8601()).toTimestamp();
        Timestamp end = TimeSpan.fromISO8601(request.getEndTimeISO8601()).toTimestamp();

        return new GetTraceListResponse(
            traceService.getTraceListSize(request.getFilters(), request.getExpression(), start, end),
            traceService.getTraceList(request.getFilters(),
                                      request.getExpression(),
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
