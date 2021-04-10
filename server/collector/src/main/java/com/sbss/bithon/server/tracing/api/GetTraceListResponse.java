package com.sbss.bithon.server.tracing.api;

import com.sbss.bithon.server.tracing.handler.TraceSpan;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 4:17 下午
 */
@Data
@AllArgsConstructor
public class GetTraceListResponse {
    private int total;
    private List<TraceSpan> rows;
}
