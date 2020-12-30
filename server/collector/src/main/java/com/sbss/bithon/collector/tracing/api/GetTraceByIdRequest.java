package com.sbss.bithon.collector.tracing.api;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 3:55 下午
 */
@Data
public class GetTraceByIdRequest {
    @NotNull
    private String traceId;

    private boolean hierachy = false;
}
