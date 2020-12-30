package com.sbss.bithon.collector.datasource.api;

import com.sbss.bithon.collector.datasource.storage.DimensionCondition;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 8:20 下午
 */
@Data
public class GetMetricsRequest {
    @NotEmpty
    private String startTimeISO8601;

    @NotEmpty
    private String endTimeISO8601;

    @NotEmpty
    private String dataSource;

    @Valid
    @Size(min=1)
    private List<DimensionCondition> dimensions;

    @Size(min = 1)
    private List<String> metrics;
}
