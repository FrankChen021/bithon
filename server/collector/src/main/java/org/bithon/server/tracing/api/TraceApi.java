/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.server.tracing.api;

import org.bithon.server.tracing.handler.TraceSpan;
import org.bithon.server.tracing.storage.ITraceReader;
import org.bithon.server.tracing.storage.ITraceStorage;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 3:53 下午
 */
@CrossOrigin
@RestController
public class TraceApi {

    private final ITraceReader traceReader;

    public TraceApi(ITraceStorage traceStorage) {
        this.traceReader = traceStorage.createReader();
    }

    @PostMapping("/api/trace/getTraceById")
    public List<TraceSpan> getTraceById(@Valid @RequestBody GetTraceByIdRequest request) {
        List<TraceSpan> spanList = traceReader.getTraceByTraceId(request.getTraceId());
        if (!request.isHierachy()) {
            return spanList;
        }

        Map<String, TraceSpanBo> boMap = spanList.stream()
                                                 .collect(Collectors.toMap(span -> span.spanId,
                                                                           val -> {
                                                                               TraceSpanBo bo = new TraceSpanBo();
                                                                               BeanUtils.copyProperties(val, bo);
                                                                               return bo;
                                                                           }));

        List<TraceSpan> rootSpans = new ArrayList<>();
        for (TraceSpan span : spanList) {
            TraceSpanBo bo = boMap.get(span.spanId);
            if (StringUtils.isEmpty(span.parentSpanId)) {
                rootSpans.add(bo);
            } else {
                TraceSpanBo parentSpan = boMap.get(span.parentSpanId);
                if (parentSpan == null) {
                    //should not happen
                } else {
                    parentSpan.children.add(bo);
                }
            }
        }
        return rootSpans;
    }

    @PostMapping("/api/trace/getTraceList")
    public GetTraceListResponse getTraceList(@Valid @RequestBody GetTraceListRequest request) {
        return new GetTraceListResponse(
            traceReader.getTraceListSize(request.getAppName()),
            traceReader.getTraceList(request.getAppName(), request.getPageNumber(), request.getPageSize())
        );
    }

    @PostMapping("/api/trace/getChildSpans")
    public List<TraceSpan> getChildSpans(@Valid @RequestBody GetChildSpansRequest request) {
        return traceReader.getTraceByParentSpanId(request.getParentSpanId());
    }
}
