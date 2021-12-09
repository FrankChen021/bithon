package org.bithon.server.metric.sink;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.common.utils.collection.CloseableIterator;

/**
 * @author Frank Chen
 * @date 9/12/21 2:17 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IMetricMessageSink {
    void process(String messageType, CloseableIterator<MetricMessage> message);
}
