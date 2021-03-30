package com.sbss.bithon.agent.core.metric.domain.mongo;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IntervalMetricCollector;

import java.util.List;

/**
 * @author frankchen
 */
public class MongoDbMetricCollector extends IntervalMetricCollector<MongoDbCompositeMetric> {

    @Override
    protected MongoDbCompositeMetric newMetrics() {
        return new MongoDbCompositeMetric();
    }

    @Override
    protected Object toMessage(IMessageConverter messageConverter,
                               int interval,
                               long timestamp,
                               List<String> dimensions,
                               MongoDbCompositeMetric metric) {
        return messageConverter.from(timestamp, interval, dimensions, metric);
    }

    public MongoDbCompositeMetric getOrCreateMetric(String server, String database) {
        return super.getOrCreateMetric(server, database);
    }

    /**
     * a temp interafce to allow mongodb-3.8 plugin to compile OK
     */
    @Deprecated
    public MongoDbCompositeMetric getOrCreateMetric(String server) {
        return super.getOrCreateMetric(server);
    }
}
