package com.sbss.bithon.server.collector.kafka;

import com.sbss.bithon.server.event.handler.EventsMessageHandler;
import com.sbss.bithon.server.metric.handler.ExceptionMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.HttpClientMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JdbcPoolMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmGcMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.RedisMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.SqlMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.ThreadPoolMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.WebRequestMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.WebServerMetricMessageHandler;
import com.sbss.bithon.server.tracing.handler.TraceMessageHandler;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
@Component
@ConditionalOnProperty(value = "collector-kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaCollectorStarter implements SmartLifecycle, ApplicationContextAware {
    ApplicationContext context;

    private final List<IKafkaCollector> collectors = new ArrayList<>();

    private static class NamedThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        /**
         * given name.
         *
         * @param name the name of the threads, to be used in the pattern {@code
         *             metrics-$NAME$-thread-$NUMBER$}
         */
        NamedThreadFactory(String name) {
            final SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = name + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void start() {
        KafkaCollectorConfig config = this.context.getBean(KafkaCollectorConfig.class);
        Map<String, Object> consumerProps = config.getConsumer();
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        collectors.add(new KafkaMetricCollector(context.getBean(JvmMetricMessageHandler.class),
                                                context.getBean(JvmGcMetricMessageHandler.class),
                                                context.getBean(WebRequestMetricMessageHandler.class),
                                                context.getBean(WebServerMetricMessageHandler.class),
                                                context.getBean(ExceptionMetricMessageHandler.class),
                                                context.getBean(HttpClientMetricMessageHandler.class),
                                                context.getBean(ThreadPoolMetricMessageHandler.class),
                                                context.getBean(JdbcPoolMetricMessageHandler.class),
                                                context.getBean(RedisMetricMessageHandler.class),
                                                context.getBean(SqlMetricMessageHandler.class))
                           .start(consumerProps));

        collectors.add(new KafkaTraceCollector(context.getBean(TraceMessageHandler.class)).start(consumerProps));
        collectors.add(new KafkaEventCollector(context.getBean(EventsMessageHandler.class)).start(consumerProps));
    }

    @Override
    public void stop() {
        for (IKafkaCollector collector : collectors) {
            collector.stop();
        }
    }

    @Override
    public boolean isRunning() {
        return collectors.stream().anyMatch(IKafkaCollector::isRunning);
    }
}
