package com.sbss.bithon.server.tracing.api;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 4:17 下午
 */
@Data
public class GetTraceListRequest {
    @NotNull
    private String appName;
}
