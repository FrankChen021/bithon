package com.sbss.bithon.server.topo.api;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/20 21:39
 */
@Data
public class GetTopoRequest {
    @NotEmpty
    private String startTimeISO8601;

    @NotEmpty
    private String endTimeISO8601;

    @NotEmpty
    private String application;

    @Min(1)
    @Max(5)
    private int callerDepth = 1;

    @Min(1)
    @Max(5)
    private int calleeDepth = 1;
}
