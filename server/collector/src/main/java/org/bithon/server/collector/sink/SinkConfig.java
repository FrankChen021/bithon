package org.bithon.server.collector.sink;

import lombok.Data;

import java.io.IOException;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 9/12/21 4:48 PM
 */
@Data
public class SinkConfig {
    private String type;
    private Map<String, Object> props;

    public static <T> T createSink(SinkConfig config, com.fasterxml.jackson.databind.ObjectMapper mapper, Class<T> clazz) throws IOException {
        String sinkConfig = mapper.writeValueAsString(config);
        return mapper.readValue(sinkConfig, clazz);
    }
}
