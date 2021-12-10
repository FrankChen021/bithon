package org.bithon.server.tracing.mapping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Frank Chen
 * @date 10/12/21 3:27 PM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TraceMapping {
    private long timestamp;
    private String userId;
    private String traceId;
}
