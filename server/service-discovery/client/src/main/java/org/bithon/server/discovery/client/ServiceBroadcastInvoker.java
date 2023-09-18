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
import org.bithon.server.discovery.declaration.ServiceResponse;
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
 * @author Frank Chen
 * @date 23/2/23 12:47 am
 */
public class ServiceBroadcastInvoker implements ApplicationContextAware {
    @Getter
    private IDiscoveryClient serviceDiscoveryClient;

    @Getter
    private final ServiceInvocationExecutor executor;

    @Getter
    private final IDiscoveryClient discoveryClient;

    private ObjectMapper objectMapper;
    private ApplicationContext applicationContext;

    public ServiceBroadcastInvoker(IDiscoveryClient discoveryClient, ServiceInvocationExecutor executor) {
        this.discoveryClient = discoveryClient;
        this.executor = executor;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.serviceDiscoveryClient = discoveryClient;
        this.objectMapper = applicationContext.getBean(ObjectMapper.class);
    }

    /**
     * Create invoker for given interface.
     *
     * @param serviceDeclaration An interface which MUST be annotated by {@link DiscoverableService}
     */
    public <T> T create(Class<T> serviceDeclaration) {
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

    private class ServiceBroadcastInvocationHandler<T> implements InvocationHandler {
        private final ObjectMapper objectMapper;
        private final Class<T> type;
        private final String serviceName;

        private ServiceBroadcastInvocationHandler(ObjectMapper objectMapper,
                                                  Class<T> type,
                                                  String name) {
            this.objectMapper = objectMapper;
            this.type = type;
            this.serviceName = name;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (serviceDiscoveryClient == null) {
                throw new HttpMappableException(HttpStatus.SERVICE_UNAVAILABLE.value(),
                                                "This API is unavailable because Service Discovery is not configured.");
            }

            // The discovered client might be the current process that enables the security module,
            // Under current security implementation, an API is needed.
            Authentication authentication = SecurityContextHolder.getContext() == null ? null : SecurityContextHolder.getContext().getAuthentication();
            Object token = authentication == null ? null : authentication.getCredentials();

            // Get all instances first
            List<IDiscoveryClient.HostAndPort> instanceList = serviceDiscoveryClient.getInstanceList(serviceName);

            //
            // Invoke remote service on each instance
            //
            List<Future<ServiceResponse<?>>> futures = new ArrayList<>(instanceList.size());
            for (IDiscoveryClient.HostAndPort hostAndPort : instanceList) {
                futures.add(executor.submit(new RemoteServiceCaller<>(objectMapper, type, hostAndPort, method, args, token)));
            }

            // Since the deserialized rows object might be unmodifiable, we always create a new array to hold the final result
            List mergedRows = new ArrayList();

            //
            // Merge the result together
            //
            for (Future<ServiceResponse<?>> future : futures) {
                try {
                    ServiceResponse<?> response = future.get();
                    if (response.getError() != null) {
                        return response;
                    }

                    // Merge response
                    mergedRows.addAll(response.getRows());
                } catch (InterruptedException | ExecutionException e) {
                    if (e.getCause() instanceof HttpMappableException) {
                        throw (HttpMappableException) e.getCause();
                    }
                    throw new RuntimeException(e);
                }
            }

            return ServiceResponse.success(mergedRows);
        }
    }

    private class RemoteServiceCaller<T> implements Callable<ServiceResponse<?>> {
        final ObjectMapper objectMapper;

        private final Class<T> type;

        private final IDiscoveryClient.HostAndPort hostAndPort;
        private final Method method;
        private final Object[] args;

        /**
         * The Bithon API token
         */
        private final Object token;

        private RemoteServiceCaller(ObjectMapper objectMapper,
                                    Class<T> type,
                                    IDiscoveryClient.HostAndPort hostAndPort,
                                    Method method,
                                    Object[] args,
                                    Object token) {
            this.objectMapper = objectMapper;
            this.type = type;
            this.hostAndPort = hostAndPort;
            this.method = method;
            this.args = args;
            Authentication authentication = SecurityContextHolder.getContext() == null ? null : SecurityContextHolder.getContext().getAuthentication();
            this.token = authentication == null ? null : authentication.getCredentials();
        }

        @Override
        public ServiceResponse<?> call() {
            Object proxyObject = Feign.builder()
                                      .contract(applicationContext.getBean(Contract.class))
                                      .encoder(applicationContext.getBean(Encoder.class))
                                      .decoder(applicationContext.getBean(Decoder.class))
                                      .errorDecoder(new ErrorResponseDecoder(objectMapper))
                                      .requestInterceptor(template -> {
                                          if (token != null) {
                                              template.header("X-Bithon-Token", token.toString());
                                          }
                                      })
                                      .target(type, "http://" + hostAndPort.getHost() + ":" + hostAndPort.getPort());

            InvocationHandler handler = Proxy.getInvocationHandler(proxyObject);
            try {
                // The remote service must return a type of Collection
                return (ServiceResponse<?>) handler.invoke(proxyObject, method, args);
            } catch (FeignException.NotFound e) {
                // Ignore the exception that the target service does not have data of given args
                // This might also ignore the HTTP layer 404 problem
                return ServiceResponse.EMPTY;
            } catch (FeignException.Forbidden e) {
                throw new HttpMappableException(HttpStatus.FORBIDDEN.value(),
                                                e.getMessage());
            } catch (HttpMappableException e) {
                // Customized ErrorDecoder decodes exception
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
