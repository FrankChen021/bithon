package com.sbss.bithon.collector.datasource.api;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 4:27 下午
 */
@Data
public class GetMetricsBySqlRequest {

    private String dataSource;

    @NotEmpty
    private String sql;
}
