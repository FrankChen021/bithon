package org.bithon.server.web.service.tracing.api;



import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.Collections;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 15/1/22 4:08 PM
 */
@Data
public class SearchTraceRequest {
    @NotBlank
    private String startTimeISO8601;

    @NotBlank
    private String endTimeISO8601;

    @NotEmpty
    private Map<String, String> conditions = Collections.emptyMap();

    private String order;
    private String orderBy;
    private int pageNumber = 0;
    private int pageSize = 10;
}
