package com.sbss.bithon.agent.core.metric;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;

import java.util.List;

/**
 * @author frankchen
 */
public interface IMetricCollector {

    boolean isEmpty();

    List<Object> collect(IMessageConverter messageConverter,
                         AppInstance appInstance,
                         int interval,
                         long timestamp);
}
