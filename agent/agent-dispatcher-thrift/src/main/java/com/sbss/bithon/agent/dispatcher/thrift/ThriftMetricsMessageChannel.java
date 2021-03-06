package com.sbss.bithon.agent.dispatcher.thrift;

import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.dispatcher.channel.IMessageChannel;
import com.sbss.bithon.agent.rpc.thrift.service.metric.IMetricCollector;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.Response;
import org.apache.thrift.protocol.TProtocol;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/6 11:40 下午
 */
public class ThriftMetricsMessageChannel implements IMessageChannel {
    static Logger log = LoggerFactory.getLogger(ThriftMetricsMessageChannel.class);

    private static final int MAX_RETRY = 3;

    private final Map<String, Method> sendMethods;
    private final AbstractThriftClient<IMetricCollector.Client> client;
    private final DispatcherConfig dispatcherConfig;

    public ThriftMetricsMessageChannel(DispatcherConfig dispatcherConfig) {
        Method[] methods = IMetricCollector.Client.class.getDeclaredMethods();
        this.sendMethods = Arrays.stream(methods)
            .filter(m -> m.getName().startsWith("send") && !m.getName().startsWith("send_")
                && m.getParameterCount() == 1)
            .collect(Collectors.toMap(m -> m.getParameterTypes()[0].getName(), method -> method));

        this.client = new AbstractThriftClient<IMetricCollector.Client>("metric",
                                                                        dispatcherConfig.getServers(),
                                                                        dispatcherConfig.getClient().getTimeout()) {
            @Override
            protected IMetricCollector.Client createClient(TProtocol protocol) {
                return new IMetricCollector.Client(protocol);
            }
        };

        this.dispatcherConfig = dispatcherConfig;
    }

    @Override
    public void sendMessage(Object message) {
        // TODO: check timestamp first

        this.client.ensureClient((client) -> {
            String className = message.getClass().getName();
            Method method = sendMethods.get(className);
            if (null == method) {
                log.error("No service method found for entity: " + className);
                return null;
            }
            try {
                boolean isDebugOn = this.dispatcherConfig.getMessageDebug().getOrDefault(message.getClass().getSimpleName(), false);
                if ( isDebugOn ) {
                    log.info("[Debugging] Sending Thrift Messages: {}", message);
                }
                return method.invoke(client, message);
            } catch (IllegalAccessException e) {
                log.warn("Failed to send metrics: []-[]", className, method);
                return null;
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getTargetException());
            }
        }, MAX_RETRY);
    }
}
