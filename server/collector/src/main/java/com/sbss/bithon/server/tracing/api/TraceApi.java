package com.sbss.bithon.server.tracing.api;

import com.sbss.bithon.server.tracing.storage.ITraceReader;
import com.sbss.bithon.server.tracing.storage.ITraceStorage;
import com.sbss.bithon.server.tracing.handler.TraceSpan;
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
    public List<TraceSpan> getTraceList(@Valid @RequestBody GetTraceListRequest request) {
        return traceReader.getTraceList(request.getAppName());
    }

    @PostMapping("/api/trace/getChildSpans")
    public List<TraceSpan> getChildSpans(@Valid @RequestBody GetChildSpansRequest request) {
        return traceReader.getTraceByParentSpanId(request.getParentSpanId());
    }
}
