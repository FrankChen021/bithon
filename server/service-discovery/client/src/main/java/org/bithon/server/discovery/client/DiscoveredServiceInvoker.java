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

package org.bithon.server.discovery.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.Feign;
import feign.FeignException;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.Getter;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Invoke the discovered service on service provider(providers) instance(s).
 *
 * @author Frank Chen
 * @date 23/2/23 12:47 am
 */
public class DiscoveredServiceInvoker implements ApplicationContextAware {
    @Getter
    private IDiscoveryClient serviceDiscoveryClient;

    @Getter
    private final ServiceInvocationExecutor executor;

    @Getter
    private final IDiscoveryClient discoveryClient;

    private ObjectMapper objectMapper;
    private ApplicationContext applicationContext;

    public DiscoveredServiceInvoker(IDiscoveryClient discoveryClient, ServiceInvocationExecutor executor) {
        this.discoveryClient = discoveryClient;
        this.executor = executor;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.serviceDiscoveryClient = discoveryClient;
        this.objectMapper = applicationContext.getBean(ObjectMapper.class);
    }

    public <T> List<DiscoveredServiceInstance> getInstanceList(Class<T> serviceDeclaration) {
        if (serviceDiscoveryClient == null) {
            throw new HttpMappableException(HttpStatus.SERVICE_UNAVAILABLE.value(),
                                            "This API is unavailable because Service Discovery is not configured.");
        }

        DiscoverableService metadata = serviceDeclaration.getAnnotation(DiscoverableService.class);
        if (metadata == null) {
            throw new RuntimeException(StringUtils.format("Given class [%s] is not marked by annotation [%s].",
                                                          serviceDeclaration.getName(),
                                                          DiscoverableService.class.getSimpleName()));
        }

        return serviceDiscoveryClient.getInstanceList(metadata.name());
    }

    /**
     * Create invoker for given interface.
     *
     * @param serviceDeclaration An interface which MUST be annotated by {@link DiscoverableService}.
     *                           And each of methods must return a type of void
     */
    public <T> T createBroadcastApi(Class<T> serviceDeclaration) {
        DiscoverableService metadata = serviceDeclaration.getAnnotation(DiscoverableService.class);
        if (metadata == null) {
            throw new RuntimeException(StringUtils.format("Given class [%s] is not marked by annotation [%s].",
                                                          serviceDeclaration.getName(),
                                                          DiscoverableService.class.getSimpleName()));
        }

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(serviceDeclaration.getClassLoader(),
                                          new Class<?>[]{serviceDeclaration},
                                          new ServiceBroadcastInvocationHandler<>(objectMapper,
                                                                                  serviceDeclaration,
                                                                                  metadata.name()));
    }

    public <T> T createUnicastApi(Class<T> serviceDeclaration, DiscoveredServiceInstance instance) {
        DiscoverableService metadata = serviceDeclaration.getAnnotation(DiscoverableService.class);
        if (metadata == null) {
            throw new RuntimeException(StringUtils.format("Given class [%s] is not marked by annotation [%s].",
                                                          serviceDeclaration.getName(),
                                                          DiscoverableService.class.getSimpleName()));
        }

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(serviceDeclaration.getClassLoader(),
                                          new Class<?>[]{serviceDeclaration},
                                          new ServiceUnicastInvocationHandler<>(serviceDeclaration,
                                                                                instance,
                                                                                objectMapper));
    }

    private class ServiceUnicastInvocationHandler<T> implements InvocationHandler {
        private final ObjectMapper objectMapper;
        private final Class<T> type;
        private final DiscoveredServiceInstance instance;

        private ServiceUnicastInvocationHandler(Class<T> type,
                                                DiscoveredServiceInstance instance,
                                                ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            this.type = type;
            this.instance = instance;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            // The discovered client might be the current process, which enables the security model.
            // Under the current security implementation, an API token is needed.
            Authentication authentication = SecurityContextHolder.getContext() == null ? null : SecurityContextHolder.getContext().getAuthentication();
            Object token = authentication == null ? null : authentication.getCredentials();

            return new RemoteServiceCaller<>(objectMapper, type, instance, method, args, token).call();
        }
    }

    private class ServiceBroadcastInvocationHandler<T> implements InvocationHandler {
        private final ObjectMapper objectMapper;
        private final Class<T> serviceDeclaration;
        private final String serviceName;

        private ServiceBroadcastInvocationHandler(ObjectMapper objectMapper,
                                                  Class<T> serviceDeclaration,
                                                  String name) {
            this.objectMapper = objectMapper;
            this.serviceDeclaration = serviceDeclaration;
            this.serviceName = name;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (serviceDiscoveryClient == null) {
                throw new HttpMappableException(HttpStatus.SERVICE_UNAVAILABLE.value(),
                                                "This API is unavailable because Service Discovery is not configured.");
            }

            Class<?> retType = method.getReturnType();
            if (!retType.equals(void.class)) {
                throw new UnsupportedOperationException(StringUtils.format("method [%s#%s] returns type of [%s] which is not supported. Only void is supported now.",
                                                                           method.getDeclaringClass().getName(),
                                                                           method.getName(),
                                                                           retType.getName()));
            }

            // The discovered client might be the current process, which enables the security model.
            // Under the current security implementation, an API token is needed.
            Authentication authentication = SecurityContextHolder.getContext() == null ? null : SecurityContextHolder.getContext().getAuthentication();
            Object token = authentication == null ? null : authentication.getCredentials();

            // Get all instances first
            List<DiscoveredServiceInstance> instanceList = serviceDiscoveryClient.getInstanceList(serviceName);

            // Invoke remote service on each instance
            List<Future<?>> futures = new ArrayList<>(instanceList.size());
            for (DiscoveredServiceInstance instance : instanceList) {
                futures.add(executor.submit(new RemoteServiceCaller<>(objectMapper, serviceDeclaration, instance, method, args, token)));
            }

            // Wait for completion
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    if (e.getCause() instanceof HttpMappableException) {
                        throw (HttpMappableException) e.getCause();
                    }
                    throw new RuntimeException(e);
                }
            }

            return null;
        }
    }

    private class RemoteServiceCaller<T, RESP> implements Callable<RESP> {
        final ObjectMapper objectMapper;

        private final Class<T> type;

        private final DiscoveredServiceInstance instance;
        private final Method method;
        private final Object[] args;

        /**
         * The Bithon API token
         */
        private final Object token;

        private RemoteServiceCaller(ObjectMapper objectMapper,
                                    Class<T> type,
                                    DiscoveredServiceInstance instance,
                                    Method method,
                                    Object[] args,
                                    Object token) {
            this.objectMapper = objectMapper;
            this.type = type;
            this.instance = instance;
            this.method = method;
            this.args = args;
            this.token = token;
        }

        @Override
        public RESP call() {
            Object feignObject = Feign.builder()
                                      .contract(applicationContext.getBean(Contract.class))
                                      .encoder(applicationContext.getBean(Encoder.class))
                                      .decoder(applicationContext.getBean(Decoder.class))
                                      .errorDecoder(new ErrorResponseDecoder(objectMapper))
                                      .requestInterceptor(template -> {
                                          if (token != null) {
                                              template.header("X-Bithon-Token", token.toString());
                                          }
                                      })
                                      .target(type, "http://" + instance.getHost() + ":" + instance.getPort());

            // The created feignObject is also a java proxy.
            // To invoke the method on the proxy object, we need to get its InvocationHandler object first.
            InvocationHandler handler = Proxy.getInvocationHandler(feignObject);

            try {
                // The remote service must return a type of Collection
                //noinspection unchecked
                return (RESP) handler.invoke(feignObject, method, args);
            } catch (FeignException.Forbidden e) {
                throw new HttpMappableException(HttpStatus.FORBIDDEN.value(), e.getMessage());
            } catch (HttpMappableException e) {
                // Customized ErrorDecoder decodes exception
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
