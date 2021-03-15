package com.sbss.bithon.server.collector.sink;

import java.util.Map;

/**
 * @author frankchen
 */
public interface IMessageSink {
    void process(String messageType, Map<String, Object> message);
}
