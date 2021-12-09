package org.bithon.server.metric.handler;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.common.utils.collection.CloseableIterator;
import org.bithon.server.metric.handler.MetricMessage;

/**
 * @author Frank Chen
 * @date 9/12/21 2:17 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IMetricMessageSink {
    void process(String messageType, CloseableIterator<MetricMessage> message);
}
