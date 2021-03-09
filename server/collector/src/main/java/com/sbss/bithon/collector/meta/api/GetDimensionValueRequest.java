package com.sbss.bithon.collector.meta.api;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/6 1:28 下午
 */
@Data
public class GetDimensionValueRequest {
    @NotNull
    private String dataSourceName;

    @NotNull
    private String dimensionName;

    @NotNull
    private String startISO8601;

    @NotNull
    private String endISO8601;
}
