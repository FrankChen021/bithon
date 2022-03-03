package org.bithon.server.tracing.index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author Frank Chen
 * @date 3/3/22 1:53 PM
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagIndex {
    private long timestamp;
    private String application;
    private String traceId;
    private String name;
    private String value;
}
