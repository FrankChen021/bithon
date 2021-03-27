package com.sbss.bithon.agent.plugin.mongodb;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IntervalMetricCollector;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoClientCompositeMetric;

import java.util.List;

/**
 * @author frankchen
 */
public class MongoDbMetricCollector extends IntervalMetricCollector<MongoClientCompositeMetric> {

    @Override
    protected MongoClientCompositeMetric newMetrics() {
        return new MongoClientCompositeMetric();
    }

    @Override
    protected Object toMessage(IMessageConverter messageConverter,
                               int interval,
                               long timestamp,
                               List<String> dimensions,
                               MongoClientCompositeMetric metric) {
        return messageConverter.from(timestamp, interval, dimensions, metric);
    }
}
