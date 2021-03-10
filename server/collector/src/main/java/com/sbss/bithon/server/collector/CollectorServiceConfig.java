package com.sbss.bithon.server.collector;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/23 11:45 下午
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "collector-service")
public class CollectorServiceConfig {
    private Map<String, Integer> port;
}
