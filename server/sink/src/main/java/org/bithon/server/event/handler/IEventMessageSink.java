package org.bithon.server.event.handler;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.common.utils.collection.CloseableIterator;
import org.bithon.server.event.handler.EventMessage;

/**
 * @author Frank Chen
 * @date 9/12/21 2:23 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IEventMessageSink {

    void process(String messageType, CloseableIterator<EventMessage> messages);
}
