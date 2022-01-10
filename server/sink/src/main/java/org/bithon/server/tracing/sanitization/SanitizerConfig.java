package org.bithon.server.tracing.sanitization;

import lombok.Data;

import java.util.Map;

/**
 * @author Frank Chen
 * @date 10/1/22 2:31 PM
 */
@Data
public class SanitizerConfig {
    private String type;
    private Map<String, Object> args;
}
