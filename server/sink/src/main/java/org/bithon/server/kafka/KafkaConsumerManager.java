/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.kafka;

import lombok.SneakyThrows;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.sink.event.EventMessageHandlers;
import org.bithon.server.sink.event.EventSinkConfig;
import org.bithon.server.sink.event.LocalEventSink;
import org.bithon.server.sink.metrics.LocalMetricSink;
import org.bithon.server.sink.metrics.MetricSinkConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public class KafkaConsumerManager implements SmartLifecycle, ApplicationContextAware {
    private ApplicationContext context;

    private final List<IKafkaConsumer> collectors = new ArrayList<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @SneakyThrows
    @Override
    public void start() {
        KafkaConsumerConfig config = this.context.getBean(KafkaConsumerConfig.class);

        if (this.context.getBean(MetricSinkConfig.class).isEnabled()) {
            Preconditions.checkNotNull(config.getMetrics(), "The metrics property of kafka collector is not configured while the metric sink is enabled.");

            collectors.add(new KafkaMetricConsumer(new LocalMetricSink(this.context), this.context)
                               .start(config.getMetrics()));
        }

        if (this.context.getBean(EventSinkConfig.class).isEnabled()) {
            Preconditions.checkNotNull(config.getEvent(), "The event property of kafka collector is not configured while the event sink is enabled.");

            collectors.add(new KafkaEventConsumer(new LocalEventSink(this.context.getBean(EventMessageHandlers.class)), this.context)
                               .start(config.getEvent()));
        }
    }

    @Override
    public void stop() {
        for (IKafkaConsumer collector : collectors) {
            collector.stop();
        }
    }

    @Override
    public boolean isRunning() {
        return collectors.stream().anyMatch(IKafkaConsumer::isRunning);
    }
}
