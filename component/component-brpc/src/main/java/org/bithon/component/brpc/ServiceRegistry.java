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

import org.bithon.component.brpc.exception.DuplicateServiceException;
import org.bithon.component.brpc.exception.ServiceRegistrationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServiceRegistry {

    /**
     * key is the service name
     * val is its methods where key is the method name, val is the registry
     */
    private final Map<String, Map<String, RegistryItem>> registry = new ConcurrentHashMap<>();

    public void addService(Object serviceImpl) {
        Set<Class<?>> interfaces = new HashSet<>();

        Class<?> clazz = serviceImpl.getClass();
        while (clazz != null) {
            interfaces.addAll(Stream.of(clazz.getInterfaces())
                                    .filter(interfaceClazz -> interfaceClazz.getAnnotation(BrpcService.class) != null)
                                    .collect(Collectors.toList()));

            clazz = clazz.getSuperclass();
        }

        if (interfaces.isEmpty()) {
            throw new ServiceRegistrationException("Service provider [%s] has no @BrpcService declaration.", serviceImpl.getClass().getName());
        }
        for (Class<?> interfaceType : interfaces) {
            addService(interfaceType, serviceImpl);
        }
    }

    private void addService(Class<?> interfaceType, Object serviceImpl) {
        for (Method method : interfaceType.getDeclaredMethods()) {
            ServiceConfiguration configuration = ServiceConfiguration.getServiceConfiguration(method);

            if (null != registry.computeIfAbsent(configuration.getServiceName(), v -> new ConcurrentHashMap<>())
                                .putIfAbsent(configuration.getMethodName(), new RegistryItem(method, serviceImpl))) {

                throw new DuplicateServiceException(interfaceType, method, configuration.getServiceName(), configuration.getMethodName());
            }
        }
    }

    public RegistryItem findServiceProvider(String serviceName, String methodName) {
        return registry.getOrDefault(serviceName, Collections.emptyMap()).get(methodName);
    }

    public static class ParameterType {
        private final Type rawType;
        private final Type messageType;

        public ParameterType(Type rawType, Type messageType) {
            this.rawType = rawType;
            this.messageType = messageType;
        }

        public Type getRawType() {
            return rawType;
        }

        public Type getMessageType() {
            return messageType;
        }
    }

    public static class RegistryItem {
        private final Method method;
        private final Object serviceImpl;
        private final boolean isOneway;
        private final Type[] parameterTypes;

        public RegistryItem(Method method, Object serviceImpl) {
            this.method = method;
            this.serviceImpl = serviceImpl;
            BrpcMethod sp = method.getAnnotation(BrpcMethod.class);
            this.isOneway = sp != null && sp.isOneway();
            this.parameterTypes = method.getGenericParameterTypes();
        }

        public Object invoke(Object[] args) throws InvocationTargetException, IllegalAccessException {
            return method.invoke(serviceImpl, args);
        }

        public boolean isOneway() {
            return isOneway;
        }

        public Type[] getParameterTypes() {
            return parameterTypes;
        }
    }
}
