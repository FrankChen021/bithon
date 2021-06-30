/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.dispatcher.netty;

import cn.bithon.rpc.channel.ClientChannel;
import cn.bithon.rpc.endpoint.EndPoint;
import cn.bithon.rpc.endpoint.RoundRobinEndPointProvider;
import cn.bithon.rpc.services.ApplicationType;
import cn.bithon.rpc.services.IMetricCollector;
import cn.bithon.rpc.services.MessageHeader;
import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.channel.IMessageChannel;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/27 20:14
 */
public class MetricMessageChannel implements IMessageChannel {
    private static final Logger log = LoggerFactory.getLogger(MetricMessageChannel.class);

    private final Map<String, Method> sendMethods = new HashMap<>();
    private final DispatcherConfig dispatcherConfig;
    private MessageHeader header;
    private final IMetricCollector metricCollector;

    public MetricMessageChannel(DispatcherConfig dispatcherConfig) {
        Method[] methods = IMetricCollector.class.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getParameterCount() != 2) {
                continue;
            }
            if (!method.getName().startsWith("send")) {
                continue;
            }
            Type[] paramTypes = method.getGenericParameterTypes();
            if (!paramTypes[0].equals(MessageHeader.class)) {
                continue;
            }

            Type metricMessageParamType = paramTypes[1];
            if (metricMessageParamType instanceof ParameterizedType) {
                Type messageType = ((ParameterizedType) metricMessageParamType).getActualTypeArguments()[0];
                if (messageType instanceof Class) {
                    sendMethods.put(((Class<?>) messageType).getName(), method);
                }
            }
        }

        List<EndPoint> endpoints = Stream.of(dispatcherConfig.getServers().split(",")).map(hostAndPort -> {
            String[] parts = hostAndPort.split(":");
            return new EndPoint(parts[0], Integer.parseInt(parts[1]));
        }).collect(Collectors.toList());
        metricCollector = new ClientChannel(new RoundRobinEndPointProvider(endpoints)).getRemoteService(IMetricCollector.class);

        this.dispatcherConfig = dispatcherConfig;

        AppInstance appInstance = AgentContext.getInstance().getAppInstance();
        this.header = MessageHeader.newBuilder()
                                   .setAppName(appInstance.getAppName())
                                   .setEnv(appInstance.getEnv())
                                   .setInstanceName(appInstance.getHostIp() + ":" + appInstance.getPort())
                                   .setHostIp(appInstance.getHostIp())
                                   .setPort(appInstance.getPort())
                                   .setAppType(ApplicationType.JAVA)
                                   .build();
        appInstance.addListener(port -> this.header = MessageHeader.newBuilder()
                                                                   .setAppName(appInstance.getAppName())
                                                                   .setEnv(appInstance.getEnv())
                                                                   .setInstanceName(appInstance.getHostIp()
                                                                                    + ":"
                                                                                    + appInstance.getPort())
                                                                   .setHostIp(appInstance.getHostIp())
                                                                   .setPort(appInstance.getPort())
                                                                   .setAppType(ApplicationType.JAVA)
                                                                   .build());
    }

    @Override
    public void sendMessage(Object message) {
        if (!(message instanceof List)) {
            return;
        }
        if (((List<?>) message).isEmpty()) {
            return;
        }

        final String messageClass = ((List<?>) message).get(0).getClass().getName();

        Method method = sendMethods.get(messageClass);
        if (null == method) {
            log.error("No service method found for entity: " + messageClass);
            return;
        }
        try {
            boolean isDebugOn = this.dispatcherConfig.getMessageDebug().getOrDefault(messageClass, false);
            if (isDebugOn) {
                log.info("[Debugging] Sending Thrift Messages: {}", message);
            }
            method.invoke(metricCollector, header, message);
        } catch (IllegalAccessException e) {
            log.warn("Failed to send metrics: []-[]", messageClass, method);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
        }
    }
}
