package org.bithon.server.tracing;

import lombok.Data;
import org.bithon.server.tracing.mapping.TraceMappingConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Frank Chen
 * @date 10/12/21 3:33 PM
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bithon.tracing")
public class TraceConfig {
    private List<TraceMappingConfig> mapping;
}
