package org.bithon.server.sink.metrics;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen
 * @date 2022/7/31 12:17
 */
@Component
public class MetricMessageHandlers {

    private final Map<String, AbstractMetricMessageHandler> handlers = new ConcurrentHashMap<>();

    public MetricMessageHandlers(ApplicationContext applicationContext) {

        Class<? extends AbstractMetricMessageHandler>[] handlers = new Class[]{
            ExceptionMetricMessageHandler.class,
            HttpIncomingMetricMessageHandler.class,
            HttpOutgoingMetricMessageHandler.class,
            JdbcPoolMetricMessageHandler.class,
            JvmMetricMessageHandler.class,
            JvmGcMetricMessageHandler.class,
            MongoDbMetricMessageHandler.class,
            RedisMetricMessageHandler.class,
            SqlMetricMessageHandler.class,
            ThreadPoolMetricMessageHandler.class,
            WebServerMetricMessageHandler.class
        };
        for (Class<? extends AbstractMetricMessageHandler> handlerClass : handlers) {
            this.add(applicationContext.getAutowireCapableBeanFactory().createBean(handlerClass));
        }
    }

    public void add(AbstractMetricMessageHandler handler) {
        handlers.put(handler.getType(), handler);
    }

    public void remove(String name) {
        handlers.remove(name);
    }

    public AbstractMetricMessageHandler getHandler(String name) {
        return handlers.get(name);
    }

    public Collection<AbstractMetricMessageHandler> getHandlers() {
        return handlers.values();
    }
}
