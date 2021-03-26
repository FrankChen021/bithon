package com.sbss.bithon.agent.core.metric.domain.sql;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IntervalMetricCollector;

import java.util.List;

/**
 * @author frankchen
 */
public class SqlMetricCollector extends IntervalMetricCollector<SqlCompositeMetric> {
    @Override
    protected SqlCompositeMetric newMetrics() {
        return new SqlCompositeMetric();
    }

    @Override
    protected Object toMessage(IMessageConverter messageConverter,
                               int interval,
                               long timestamp,
                               List<String> dimensions,
                               SqlCompositeMetric metric) {
        return messageConverter.from(timestamp, interval, dimensions, metric);
    }
}
