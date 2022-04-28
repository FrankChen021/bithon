package org.bithon.server.collector.source.http;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Set;

/**
 * @author Frank Chen
 * @date 28/4/22 4:29 PM
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "collector-http.tracing")
public class TraceHttpCollectorConfig {
    private Set<String> clickHouseApplications = Collections.singleton("clickhouse");
}
