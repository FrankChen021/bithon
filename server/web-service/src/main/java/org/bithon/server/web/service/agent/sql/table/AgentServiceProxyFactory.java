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

package org.bithon.server.web.service.agent.sql.table;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.brpc.channel.IBrpcChannel;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.exception.SessionNotFoundException;
import org.bithon.component.brpc.invocation.InvocationManager;
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.brpc.message.in.ServiceResponseMessageIn;
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.discovery.client.DiscoveredServiceInstance;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.discovery.client.ErrorResponseDecoder;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.bithon.server.discovery.declaration.controller.IAgentControllerApi;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A factory that creates a proxy to call Agent side BRPC service over an HTTP-based proxy server
 *
 * @author frank.chen021@outlook.com
 * @date 2023/4/9 16:27
 */
@Slf4j
public class AgentServiceProxyFactory {

    private final InvocationManager invocationManager = new InvocationManager();
    private final DiscoveredServiceInvoker discoveryServiceInvoker;
    private final ApplicationContext applicationContext;

    public AgentServiceProxyFactory(DiscoveredServiceInvoker discoveryServiceInvoker,
                                    ApplicationContext applicationContext) {
        this.discoveryServiceInvoker = discoveryServiceInvoker;
        this.applicationContext = applicationContext;
    }

    /**
     * @param context                 The context that contains extra invocation information
     * @param agentServiceDeclaration The service located at agent side that we want to invoke
     */
    public <T> T create(Map<String, Object> context,
                        Class<T> agentServiceDeclaration) {
        // instance is a mandatory parameter
        String instance = (String) context.get(IAgentControllerApi.PARAMETER_NAME_INSTANCE);
        Preconditions.checkNotNull(instance, "'%s' is not given in the context.", IAgentControllerApi.PARAMETER_NAME_INSTANCE);

        // Locate the proxy server
        DiscoverableService metadata = IAgentControllerApi.class.getAnnotation(DiscoverableService.class);
        if (metadata == null) {
            throw new RuntimeException(StringUtils.format("Given class [%s] is not marked by annotation [%s].",
                                                          IAgentControllerApi.class.getName(),
                                                          DiscoverableService.class.getSimpleName()));
        }

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(agentServiceDeclaration.getClassLoader(),
                                          new Class<?>[]{agentServiceDeclaration},
                                          new AgentServiceBroadcastInvoker(IAgentControllerApi.class,
                                                                           context,
                                                                           invocationManager));
    }

    /**
     * Invoke the agent service on ALL proxy servers,
     * and the proxy server is responsible for invoking agent service on given agent
     */
    private class AgentServiceBroadcastInvoker implements InvocationHandler {

        private final InvocationManager invocationManager;
        private final Class<?> proxyService;
        private final Map<String, Object> context;

        private AgentServiceBroadcastInvoker(Class<?> proxyService,
                                             Map<String, Object> context,
                                             InvocationManager invocationManager) {
            this.proxyService = proxyService;
            this.invocationManager = invocationManager;

            // Make sure the context is modifiable because we're going to add token into the context
            this.context = new TreeMap<>(context);
        }

        @Override
        public Object invoke(Object object,
                             Method agentServiceMethod,
                             Object[] args) throws Throwable {
            // Since the real invocation is issued from a dedicated thread-pool,
            // to make sure the task in that thread pool can access the security context, we have to explicitly
            Authentication authentication = SecurityContextHolder.getContext() == null ? null : SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() != null) {
                context.put("X-Bithon-Token", authentication.getCredentials());
            }

            // Get all service provider instance from the service discovery center
            List<DiscoveredServiceInstance> proxyServerList = discoveryServiceInvoker.getInstanceList(proxyService);

            //
            // Invoke remote service on each proxy server
            //
            List<Future<Collection<?>>> futures = new ArrayList<>(proxyServerList.size());
            for (DiscoveredServiceInstance proxyServer : proxyServerList) {
                futures.add(discoveryServiceInvoker.getExecutor().submit(() -> {
                    try {
                        // The agent's Brpc services MUST return a type of Collection
                        return (Collection<?>) invocationManager.invoke("bithon-webservice",
                                                                        Headers.EMPTY,
                                                                        new BrpcChannelOverHttp(proxyServer, context),
                                                                        30_000,
                                                                        agentServiceMethod,
                                                                        args);
                    } catch (HttpMappableException e) {
                        if (SessionNotFoundException.class.getName().equals(e.getCauseExceptionClass())) {
                            // We're issuing broadcast invocations on all proxy servers,
                            // but there will be only one proxy server that connects to the target agent instance.
                            // For any other proxy servers, a SessionNotFoundException is thrown which should be ignored.
                            return Collections.emptyList();
                        }
                        throw e;
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }));
            }

            // Since the deserialized rows object might be unmodifiable, we always create a new array to hold the final result
            //noinspection rawtypes
            List mergedResults = new ArrayList<>();

            //
            // Merge the result
            //
            for (Future<Collection<?>> future : futures) {
                try {
                    Collection<?> response = future.get();

                    // Merge response
                    // noinspection unchecked
                    mergedResults.addAll(response);
                } catch (InterruptedException | ExecutionException e) {
                    if (e.getCause() instanceof RuntimeException) {
                        throw e.getCause();
                    }
                    throw new RuntimeException(e);
                }
            }

            return mergedResults;
        }
    }

    class BrpcChannelOverHttp implements IBrpcChannel {
        private final DiscoveredServiceInstance proxyHost;
        private final Map<String, Object> context;

        public BrpcChannelOverHttp(DiscoveredServiceInstance proxyHost,
                                   Map<String, Object> context) {
            this.proxyHost = proxyHost;
            this.context = context;
        }

        @Override
        public long getConnectionLifeTime() {
            return 0;
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public boolean isWritable() {
            return true;
        }

        @Override
        public EndPoint getRemoteAddress() {
            return new EndPoint(proxyHost.getHost(), proxyHost.getPort());
        }

        @Override
        public void writeAsync(ServiceRequestMessageOut serviceRequest) throws IOException {
            final long txId = serviceRequest.getTransactionId();

            // Turn the message into a byte array to send over HTTP
            final byte[] message = serviceRequest.toByteArray();

            // The underlying call on remote HTTP endpoint is synchronous,
            // However, this writeMessage is an async operation,
            // we use CompletableFuture to turn the sync operation into async
            CompletableFuture.supplyAsync(() -> {
                                              IAgentControllerApi proxyApi = Feign.builder()
                                                                                  .contract(applicationContext.getBean(Contract.class))
                                                                                  .encoder(applicationContext.getBean(Encoder.class))
                                                                                  .decoder(applicationContext.getBean(Decoder.class))
                                                                                  .errorDecoder(new ErrorResponseDecoder(applicationContext.getBean(ObjectMapper.class)))
                                                                                  .requestInterceptor(template -> {
                                                                                      Object token = context.get("X-Bithon-Token");
                                                                                      if (token != null) {
                                                                                          template.header("X-Bithon-Token", token.toString());
                                                                                      }
                                                                                  })
                                                                                  .target(IAgentControllerApi.class, "http://" + proxyHost.getHost() + ":" + proxyHost.getPort());

                                              try {
                                                  return proxyApi.callAgentService((String) context.getOrDefault("_token", ""),
                                                                                   (String) context.getOrDefault(IAgentControllerApi.PARAMETER_NAME_INSTANCE, ""),
                                                                                   30_000,
                                                                                   message);
                                              } catch (IOException e) {
                                                  throw new RuntimeException(e);
                                              }
                                          },
                                          discoveryServiceInvoker.getExecutor())
                             .thenAccept((responseBytes) -> {
                                 try {
                                     ServiceResponseMessageIn in = ServiceResponseMessageIn.from(new ByteArrayInputStream(responseBytes));
                                     invocationManager.onResponse(in);
                                 } catch (IOException e) {
                                     invocationManager.onClientException(txId, e);
                                 }
                             })
                             .whenComplete((v, ex) -> {
                                 if (ex != null) {
                                     invocationManager.onClientException(txId, ex.getCause() != null ? ex.getCause() : ex);
                                 }
                             });
        }
    }
}
