package org.bithon.server.web.service.api;

import lombok.Data;
import org.bithon.server.metric.storage.DimensionCondition;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Frank Chen
 * @date 24/3/22 11:42 PM
 */
@Data
public class GetDimensionRequestV2 {

    @NotEmpty
    private String startTimeISO8601;

    @NotEmpty
    private String endTimeISO8601;

    @NotNull
    private String dataSource;

    @Valid
    private Collection<DimensionCondition> filters = Collections.emptyList();

    @NotNull
    private String name;

    /**
     * Indicate the type of {@link #name}
     * name
     * alias
     */
    private String type = "name";
}