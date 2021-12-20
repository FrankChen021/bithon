package org.bithon.server.web.service.tracing.api;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 17/12/21 4:06 PM
 */
@Data
@AllArgsConstructor
public class GetTraceDistributionResponse {
    private List<Map<String, Object>> data;

    /**
     * time bucket, in second
     */
    private int bucket;
}
