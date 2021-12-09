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

package org.bithon.server.web.service.tracing.service;

import org.bithon.server.web.service.tracing.api.TraceMap;
import org.bithon.server.web.service.tracing.api.TraceSpanBo;
import org.bithon.server.tracing.handler.TraceSpan;
import org.bithon.server.tracing.storage.ITraceReader;
import org.bithon.server.tracing.storage.ITraceStorage;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 24/11/21 7:11 pm
 */
@Service
public class TraceService {

    private final ITraceReader traceReader;

    public TraceService(ITraceStorage traceStorage) {
        this.traceReader = traceStorage.createReader();
    }

    public TraceMap buildMap(List<TraceSpan> spans) {
        return new TraceMapBuilder().buildMap(spans);
    }

    public List<TraceSpan> getTraceByParentSpanId(String parentSpanId) {
        return traceReader.getTraceByParentSpanId(parentSpanId);
    }

    public List<TraceSpan> getTraceByTraceId(String traceId, boolean asTree) {
        List<TraceSpan> spans = traceReader.getTraceByTraceId(traceId);

        if (!asTree) {
            return spans;
        }

        //
        // build as tree
        //
        Map<String, TraceSpanBo> boMap = spans.stream()
                .collect(Collectors.toMap(span -> span.spanId,
                        val -> {
                            TraceSpanBo bo = new TraceSpanBo();
                            BeanUtils.copyProperties(val, bo);
                            return bo;
                        }));

        List<TraceSpan> rootSpans = new ArrayList<>();
        for (TraceSpan span : spans) {
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

    public int getTraceListSize(String appName) {
        return traceReader.getTraceListSize(appName);
    }

    public List<TraceSpan> getTraceList(String appName, int pageNumber, int pageSize) {
        return traceReader.getTraceList(appName, pageNumber, pageSize);
    }
}
