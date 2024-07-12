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

package org.bithon.agent.dispatcher.brpc;

import org.bithon.agent.observability.context.AppInstance;
import org.bithon.agent.observability.dispatcher.channel.IMessageChannel;
import org.bithon.agent.observability.dispatcher.config.DispatcherConfig;
import org.bithon.agent.rpc.brpc.ApplicationType;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.metrics.IMetricCollector;
import org.bithon.component.brpc.IServiceController;
import org.bithon.component.brpc.channel.BrpcClient;
import org.bithon.component.brpc.channel.BrpcClientBuilder;
import org.bithon.component.brpc.channel.IBrpcChannel;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.endpoint.RoundRobinEndPointProvider;
import org.bithon.component.brpc.exception.CalleeSideException;
import org.bithon.component.brpc.exception.CallerSideException;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/27 20:14
 */
public class BrpcMetricMessageChannel implements IMessageChannel {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(BrpcMetricMessageChannel.class);

    private final Map<String, Method> sendMethods = new HashMap<>();
    private final DispatcherConfig dispatcherConfig;

    private final BrpcClient brpcClient;
    private IMetricCollector metricCollector;
    private BrpcMessageHeader header;

    public BrpcMetricMessageChannel(DispatcherConfig dispatcherConfig) {
        Method[] methods = IMetricCollector.class.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getParameterCount() != 2) {
                continue;
            }
            if (!method.getName().startsWith("send")) {
                continue;
            }
            Type[] paramTypes = method.getGenericParameterTypes();
            if (!paramTypes[0].equals(BrpcMessageHeader.class)) {
                continue;
            }

            Type metricMessageParamType = paramTypes[1];
            if (metricMessageParamType instanceof ParameterizedType) {
                // the 2nd parameter is something like List<BrpcXXXMessage>
                //
                // TODO: Change definition of List<BrpcMessage> into BrpcBatchMessage
                //
                Type messageType = ((ParameterizedType) metricMessageParamType).getActualTypeArguments()[0];
                if (messageType instanceof Class) {
                    sendMethods.put(((Class<?>) messageType).getName(), method);
                }
            } else {
                //TODO: check Class of the 2nd parameter is subclass of protobuf 'Message'
                sendMethods.put(paramTypes[1].getTypeName(), method);
            }
        }

        List<EndPoint> endpoints = Stream.of(dispatcherConfig.getServers().split(",")).map(hostAndPort -> {
            String[] parts = hostAndPort.split(":");
            return new EndPoint(parts[0], Integer.parseInt(parts[1]));
        }).collect(Collectors.toList());
        this.brpcClient = BrpcClientBuilder.builder()
                                           .clientId("metrics")
                                           .server(new RoundRobinEndPointProvider(endpoints))
                                           .maxRetry(3)
                                           .retryInterval(Duration.ofMillis(100))
                                           .connectionTimeout(dispatcherConfig.getClient().getConnectionTimeout())
                                           .build();
        this.dispatcherConfig = dispatcherConfig;

        AppInstance appInstance = AppInstance.getInstance();
        this.header = BrpcMessageHeader.newBuilder()
                                       .setAppName(appInstance.getQualifiedName())
                                       .setEnv(appInstance.getEnv())
                                       .setInstanceName(appInstance.getInstanceName())
                                       .setAppType(ApplicationType.JAVA)
                                       .build();
        appInstance.addListener(port -> this.header = BrpcMessageHeader.newBuilder()
                                                                       .setAppName(appInstance.getQualifiedName())
                                                                       .setEnv(appInstance.getEnv())
                                                                       .setInstanceName(appInstance.getInstanceName())
                                                                       .setAppType(ApplicationType.JAVA)
                                                                       .build());
    }

    @Override
    public void sendMessage(Object message) {
        if (this.metricCollector == null) {
            try {
                this.metricCollector = brpcClient.getRemoteService(IMetricCollector.class);
            } catch (ServiceInvocationException e) {
                LOG.warn("Unable to get remote IMetricCollector service: {}", e.getMessage());
                return;
            }
        }

        final String messageClass;
        if ((message instanceof List)) {
            if (((List<?>) message).isEmpty()) {
                return;
            }

            messageClass = ((List<?>) message).get(0).getClass().getName();
        } else {
            messageClass = message.getClass().getName();
        }

        IBrpcChannel channel = ((IServiceController) metricCollector).getChannel();
        if (channel.getConnectionLifeTime() > dispatcherConfig.getClient().getConnectionLifeTime()) {
            LOG.info("Disconnect metric-channel for client-side load balancing...");
            try {
                channel.disconnect();
            } catch (Exception ignored) {
            }
        }

        Method method = sendMethods.get(messageClass);
        if (null == method) {
            LOG.error("No service method found for entity: " + messageClass);
            return;
        }
        try {
            boolean isDebugOn = this.dispatcherConfig.getMessageDebug().getOrDefault(messageClass, false);
            if (isDebugOn) {
                LOG.info("[Debugging] Sending Metric Messages: {}", message);
            }
            method.invoke(metricCollector, header, message);
        } catch (IllegalAccessException e) {
            LOG.warn("Failed to send metrics: []-[]", messageClass, method);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof CallerSideException
                || e.getTargetException() instanceof CalleeSideException) {
                //suppress client exception
                LOG.error("Failed to send metric: {}", e.getTargetException().getMessage());
            } else {
                throw new RuntimeException(e.getTargetException());
            }
        }
    }

    @Override
    public void close() {
        this.brpcClient.close();
    }
}
