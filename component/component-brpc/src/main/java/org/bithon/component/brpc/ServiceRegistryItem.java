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

package org.bithon.component.brpc;

import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.brpc.message.serializer.Serializer;
import org.bithon.shaded.io.netty.util.internal.StringUtil;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * metadata of a method in remote service
 *
 * @author frank.chen021@outlook.com
 * @date 20/10/21 9:20 pm
 */
public class ServiceRegistryItem {
    private final String serviceName;
    private final String methodName;
    private final boolean isOneway;
    private final boolean isStreaming;
    private final Serializer serializer;
    private final Type streamingDataType;

    private final int messageType;

    public ServiceRegistryItem(String serviceName,
                               String methodName,
                               boolean isOneway,
                               boolean isStreaming,
                               int messageType,
                               Serializer serializer,
                               Type streamingDataType) {
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.isOneway = isOneway;
        this.isStreaming = isStreaming;
        this.messageType = messageType;
        this.serializer = serializer;
        this.streamingDataType = streamingDataType;
    }

    public static ServiceRegistryItem create(Method method) {
        BrpcService serviceConfig = method.getDeclaringClass().getAnnotation(BrpcService.class);
        BrpcMethod methodConfig = method.getAnnotation(BrpcMethod.class);

        String methodName;
        boolean isOneway;
        Serializer serializer;
        boolean isStreaming = false;
        Type streamingDataType = null;
        int messageType;

        // Check if this is a streaming method
        if (method.getReturnType() == void.class && method.getParameterCount() > 0) {
            Class<?> lastParamType = method.getParameterTypes()[method.getParameterCount() - 1];
            if (lastParamType == StreamResponse.class) {
                isStreaming = true;
                // Get the generic type of StreamResponse<T>
                Type lastParamGenericType = method.getGenericParameterTypes()[method.getParameterCount() - 1];
                if (lastParamGenericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) lastParamGenericType;
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length > 0) {
                        streamingDataType = actualTypeArguments[0];
                    }
                }
            }
        }

        if (methodConfig != null && !StringUtil.isNullOrEmpty(methodConfig.name())) {
            methodName = methodConfig.name();
        } else {
            methodName = method.getName();
        }

        if (methodConfig != null) {
            isOneway = methodConfig.isOneway();
        } else {
            isOneway = serviceConfig != null && serviceConfig.isOneway();
        }

        if (methodConfig != null) {
            serializer = methodConfig.serializer();
        } else {
            serializer = serviceConfig == null ? Serializer.PROTOBUF : serviceConfig.serializer();
        }

        // Determine message type
        if (isStreaming) {
            messageType = ServiceMessageType.CLIENT_STREAMING_REQUEST;
        } else {
            messageType = methodConfig == null ? ServiceMessageType.CLIENT_REQUEST_V2 : methodConfig.messageType();
        }

        return new ServiceRegistryItem(getServiceName(method.getDeclaringClass()),
                                       methodName,
                                       isOneway,
                                       isStreaming,
                                       messageType,
                                       serializer,
                                       streamingDataType);
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public boolean isOneway() {
        return isOneway;
    }

    public boolean isStreaming() {
        return isStreaming;
    }

    public Type getStreamingDataType() {
        return streamingDataType;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getMessageType() {
        return messageType;
    }

    public static String getServiceName(Class<?> serviceDeclaring) {
        BrpcService serviceConfig = serviceDeclaring.getAnnotation(BrpcService.class);
        return serviceConfig != null && !StringUtil.isNullOrEmpty(serviceConfig.name())
               ? serviceConfig.name()
               : serviceDeclaring.getName();
    }
}
