package com.sbss.bithon.server.collector.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "collector-kafka")
public class KafkaCollectorConfig {
    private Map<String, Object> consumer;
}
