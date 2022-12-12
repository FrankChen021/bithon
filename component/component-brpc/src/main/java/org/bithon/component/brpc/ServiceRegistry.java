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

/**
 * @author frankchen
 */
public class ServiceRegistry implements IServiceRegistry {

    /**
     * key is the service name
     * val is its methods where key is the method name, val is the registry
     */
    private final Map<String, Map<String, ServiceInvoker>> registry = new ConcurrentHashMap<>();

    public ServiceRegistry() {
        // register self as a service provider
        addService(this);
    }

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
            ServiceRegistryItem registryItem = ServiceRegistryItem.create(method);

            if (null != registry.computeIfAbsent(registryItem.getServiceName(), v -> new ConcurrentHashMap<>(7))
                                .putIfAbsent(registryItem.getMethodName(), new ServiceInvoker(serviceImpl, method, registryItem.isOneway()))) {

                throw new DuplicateServiceException(interfaceType,
                                                    method,
                                                    registryItem.getServiceName(),
                                                    registryItem.getMethodName());
            }
        }
    }

    public ServiceInvoker findServiceInvoker(String serviceName, String methodName) {
        return registry.getOrDefault(serviceName, Collections.emptyMap()).get(methodName);
    }

    @Override
    public boolean contains(String service) {
        return registry.containsKey(service);
    }

    public static class ServiceInvoker {
        private final Method method;
        private final Object serviceImpl;
        private final boolean isOneway;
        private final Type[] parameterTypes;

        public ServiceInvoker(Object serviceImpl, Method method, boolean isOneway) {
            this.method = method;
            this.serviceImpl = serviceImpl;
            this.isOneway = isOneway;
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
