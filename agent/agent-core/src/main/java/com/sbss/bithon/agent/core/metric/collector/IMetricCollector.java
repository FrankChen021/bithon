package com.sbss.bithon.agent.core.metric.collector;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;

import java.util.List;

/**
 * @author frankchen
 */
public interface IMetricCollector {

    boolean isEmpty();

    List<Object> collect(IMessageConverter messageConverter,
                         int interval,
                         long timestamp);
}
