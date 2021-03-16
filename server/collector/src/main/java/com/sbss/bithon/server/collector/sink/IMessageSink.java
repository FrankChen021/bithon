package com.sbss.bithon.server.collector.sink;

import java.util.Map;

/**
 * @author frankchen
 */
public interface IMessageSink<MSG> {
    void process(String messageType, MSG message);
}
