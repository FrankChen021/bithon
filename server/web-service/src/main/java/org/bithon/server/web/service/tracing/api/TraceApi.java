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

import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.matcher.StringEqualMatcher;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.sink.tracing.TraceConfig;
import org.bithon.server.storage.metrics.DimensionFilter;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.storage.tracing.ITraceReader;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.tracing.service.TraceService;
import org.bithon.server.web.service.tracing.service.TraceTopoBuilder;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 3:53 下午
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class TraceApi {

    private final TraceConfig traceConfig;
    private final TraceService traceService;

    public TraceApi(TraceConfig traceConfig, TraceService traceService) {
        this.traceConfig = traceConfig;
        this.traceService = traceService;
    }

    @PostMapping("/api/trace/getTraceById")
    public GetTraceByIdResponse getTraceById(@Valid @RequestBody GetTraceByIdRequest request) {
        List<TraceSpan> spanList = traceService.getTraceByTraceId(request.getId(),
                                                                  request.getType(),
                                                                  request.getStartTimeISO8601(),
                                                                  request.getEndTimeISO8601(),
                                                                  request.isAsTree());

        return new GetTraceByIdResponse(spanList,
                                        new TraceTopoBuilder().build(request.isAsTree() ? spanList : traceService.asTree(spanList)));
    }

    @Deprecated
    @PostMapping("/api/trace/getTraceDistribution/v2")
    public List<ITraceReader.Histogram> getTraceDistributionV2(@Valid @RequestBody GetTraceDistributionRequest request) {
        TimeSpan start = TimeSpan.fromISO8601(request.getStartTimeISO8601());
        TimeSpan end = TimeSpan.fromISO8601(request.getEndTimeISO8601());

        return traceService.getTraceDistributionV2(request.getFilters(),
                                                   start,
                                                   end);
    }

    @PostMapping("/api/trace/getTraceList")
    public GetTraceListResponse getTraceList(@Valid @RequestBody GetTraceListRequest request) {
        Timestamp start = TimeSpan.fromISO8601(request.getStartTimeISO8601()).toTimestamp();
        Timestamp end = TimeSpan.fromISO8601(request.getEndTimeISO8601()).toTimestamp();

        // backward compatibility
        if (StringUtils.hasText(request.getApplication())) {
            request.setFilters(new ArrayList<>(request.getFilters()));
            request.getFilters().add(new DimensionFilter("appName", new StringEqualMatcher(request.getApplication())));
        }

        // check if filters exists
        for (IFilter filter : request.getFilters()) {
            if (filter.getName().startsWith("tags.")) {
                String tagName = filter.getName().substring("tags.".length());
                Preconditions.checkIf(traceConfig.getIndexes().getColumnPos(tagName) > 0,
                                      "Can't search on tag [%s] because there's no index defined for this tag.",
                                      tagName);
            }
        }

        return new GetTraceListResponse(
            traceService.getTraceListSize(request.getFilters(), start, end),
            traceService.getTraceList(request.getFilters(),
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
