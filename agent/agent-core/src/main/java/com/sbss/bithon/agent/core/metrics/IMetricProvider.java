package com.sbss.bithon.agent.core.metrics;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;

import java.util.List;

/**
 * @author frankchen
 */
public interface IMetricProvider {

    boolean isEmpty();

    List<Object> buildMessages(IMessageConverter messageConverter,
                               AppInstance appInstance,
                               int interval,
                               long timestamp);
}
