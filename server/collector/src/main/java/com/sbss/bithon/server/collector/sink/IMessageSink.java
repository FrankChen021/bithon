package com.sbss.bithon.server.collector.sink;

/**
 * @author frankchen
 */
public interface IMessageSink<MSG> {
    void process(String messageType, MSG message);
}
