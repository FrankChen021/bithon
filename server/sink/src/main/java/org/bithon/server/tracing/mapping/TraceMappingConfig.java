package org.bithon.server.tracing.mapping;

import lombok.Data;

import java.util.Map;

/**
 * Extract a user-defined transaction id on a given parameter to trace id
 *
 * - type: xxx
 * - params:
 *
 * @author Frank Chen
 * @date 10/12/21 3:08 PM
 */
@Data
public class TraceMappingConfig {
    private String type;
    private Map<String, Object> args;
}
