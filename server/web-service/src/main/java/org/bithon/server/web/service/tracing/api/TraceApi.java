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

import org.bithon.component.brpc.exception.BadRequestException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.common.matcher.EqualMatcher;
import org.bithon.server.common.utils.datetime.TimeSpan;
import org.bithon.server.metric.storage.DimensionCondition;
import org.bithon.server.tracing.TraceConfig;
import org.bithon.server.tracing.TraceDataSourceSchema;
import org.bithon.server.tracing.sink.TraceSpan;
import org.bithon.server.web.service.tracing.service.TraceService;
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
                                                                  request.isHierachy());

        return new GetTraceByIdResponse(spanList, traceService.buildMap(spanList));
    }

    @PostMapping("/api/trace/getTraceDistribution")
    public GetTraceDistributionResponse getTraceDistribution(@Valid @RequestBody GetTraceDistributionRequest request) {
        // backward compatibility
        if (StringUtils.hasText(request.getApplication())) {
            request.setFilters(new ArrayList<>(request.getFilters()));
            request.getFilters().add(new DimensionCondition("appName", new EqualMatcher(request.getApplication())));
        }

        return traceService.getTraceDistribution(request.getFilters(),
                                                 request.getStartTimeISO8601(),
                                                 request.getEndTimeISO8601());
    }

    @PostMapping("/api/trace/getTraceList")
    public GetTraceListResponse getTraceList(@Valid @RequestBody GetTraceListRequest request) {
        Timestamp start = TimeSpan.fromISO8601(request.getStartTimeISO8601()).toTimestamp();
        Timestamp end = TimeSpan.fromISO8601(request.getEndTimeISO8601()).toTimestamp();

        // backward compatibility
        if (StringUtils.hasText(request.getApplication())) {
            request.setFilters(new ArrayList<>(request.getFilters()));
            request.getFilters().add(new DimensionCondition("appName", new EqualMatcher(request.getApplication())));
        }

        // check if filters exists
        for (DimensionCondition filter : request.getFilters()) {
            if (filter.getDimension().startsWith("tags.")) {
                String tagName = filter.getDimension().substring("tags".length());
                if (traceConfig.getTagIndexConfig().getColumnPos(tagName) == 0) {
                    throw new BadRequestException("Can't search on tag [%s] because there's no index defined for this tag.", tagName);
                }
            } else {
                if (TraceDataSourceSchema.getSchema().getDimensionSpecByName(filter.getDimension()) == null) {
                    throw new BadRequestException("There's no dimension [%s] defined.", filter.getDimension());
                }
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
