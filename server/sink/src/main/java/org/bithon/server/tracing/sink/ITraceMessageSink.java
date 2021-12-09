package org.bithon.server.tracing.sink;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.common.utils.collection.CloseableIterator;

/**
 * @author Frank Chen
 * @date 9/12/21 2:22 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ITraceMessageSink {
    void process(String messageType, CloseableIterator<TraceSpan> messages);
}
