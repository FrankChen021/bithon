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
import feign.codec.ErrorDecoder;
import org.bithon.component.brpc.channel.IChannelWriter;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.invocation.InvocationManager;
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.brpc.message.in.ServiceResponseMessageIn;
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.discovery.client.IDiscoveryClient;
import org.bithon.server.discovery.client.ServiceInvocationExecutor;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.shaded.com.google.protobuf.CodedOutputStream;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/9 16:27
 */
public class AgentCommandFactory {

    private final InvocationManager invocationManager = new InvocationManager();
    private final IDiscoveryClient serviceDiscoveryClient;
    private final ServiceInvocationExecutor executor;
    private final ApplicationContext applicationContext;

    public AgentCommandFactory(IDiscoveryClient discoveryClient,
                               ServiceInvocationExecutor executor,
                               ApplicationContext applicationContext) {
        this.serviceDiscoveryClient = discoveryClient;
        this.executor = executor;
        this.applicationContext = applicationContext;
    }

    public <T> T create(Class<?> proxyServiceDeclaration,
                        String agentId,
                        Class<T> serviceDeclaration) {
        DiscoverableService metadata = proxyServiceDeclaration.getAnnotation(DiscoverableService.class);
        if (metadata == null) {
            throw new RuntimeException(StringUtils.format("Given class [%s] is not marked by annotation [%s].",
                                                          proxyServiceDeclaration.getName(),
                                                          DiscoverableService.class.getSimpleName()));
        }

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(serviceDeclaration.getClassLoader(),
                                          new Class<?>[]{serviceDeclaration},
                                          new AgentCommandBroadcastInvoker<>(metadata.name(),
                                                                             agentId,
                                                                             applicationContext,
                                                                             invocationManager));
    }

    private class AgentCommandBroadcastInvoker<T> implements InvocationHandler {

        private final InvocationManager invocationManager;
        private final String proxyServiceName;
        private final String agentId;
        private final ApplicationContext applicationContext;

        private AgentCommandBroadcastInvoker(String proxyServiceName,
                                             String agentId,
                                             ApplicationContext applicationContext,
                                             InvocationManager invocationManager) {
            this.proxyServiceName = proxyServiceName;
            this.invocationManager = invocationManager;
            this.agentId = agentId;
            this.applicationContext = applicationContext;
        }

        @Override
        public Object invoke(Object proxy,
                             Method method,
                             Object[] args) throws Throwable {
            if (serviceDiscoveryClient == null) {
                throw new HttpMappableException(HttpStatus.SERVICE_UNAVAILABLE.value(),
                                                "This API is unavailable because Service Discovery is not configured.");
            }

            // Get all instances first
            List<IDiscoveryClient.HostAndPort> instanceList = serviceDiscoveryClient.getInstanceList(proxyServiceName);

            //
            // Invoke remote service on each instance
            //
            List<Future<Collection<?>>> futures = new ArrayList<>(instanceList.size());
            for (IDiscoveryClient.HostAndPort hostAndPort : instanceList) {
                futures.add(executor.submit(() -> {
                    try {
                        return (Collection<?>) invocationManager.invoke("",
                                                                        Headers.EMPTY,
                                                                        new ProxyChannel(hostAndPort, agentId),
                                                                        30_000,
                                                                        method,
                                                                        args);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }));
            }

            // Since the deserialized rows object might be unmodifiable, we always create a new array to hold the final result
            List mergedRows = new ArrayList<>();

            //
            // Merge the result together
            //
            for (Future<Collection<?>> future : futures) {
                try {
                    Collection<?> response = future.get();

                    // Merge response
                    mergedRows.addAll(response);
                } catch (InterruptedException | ExecutionException e) {
                    if (e.getCause() instanceof HttpMappableException) {
                        throw e.getCause();
                    }
                    throw new RuntimeException(e);
                }
            }

            return mergedRows;
        }
    }

    class ProxyChannel implements IChannelWriter {
        private final IDiscoveryClient.HostAndPort proxyHost;
        private final String agentId;

        public ProxyChannel(IDiscoveryClient.HostAndPort proxyHost,
                            String agentId) {
            this.proxyHost = proxyHost;
            this.agentId = agentId;
        }

        @Override
        public void connect() {
        }

        @Override
        public void disconnect() {
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
        public void write(Object obj) throws IOException {
            ServiceRequestMessageOut serviceRequest = (ServiceRequestMessageOut) obj;
            final long txId = serviceRequest.getTransactionId();
            final byte[] message = toByteArray(serviceRequest);

            CompletableFuture.supplyAsync(() -> {
                                              IAgentCommandApi proxyObject = Feign.builder()
                                                                                  .contract(applicationContext.getBean(Contract.class))
                                                                                  .encoder(applicationContext.getBean(Encoder.class))
                                                                                  .decoder(applicationContext.getBean(Decoder.class))
                                                                                  .errorDecoder((methodKey, response) -> {
                                                                                      try {
                                                                                          ServiceResponse.Error error = applicationContext.getBean(ObjectMapper.class).readValue(response.body().asInputStream(), ServiceResponse.Error.class);
                                                                                          return new HttpMappableException(response.status(), "Exception from remote [%s]: %s", proxyHost, error.getMessage());
                                                                                      } catch (IOException ignored) {
                                                                                      }

                                                                                      // Delegate to default decoder
                                                                                      return new ErrorDecoder.Default().decode(methodKey, response);
                                                                                  })
                                                                                  .target(IAgentCommandApi.class, "http://" + proxyHost.getHost() + ":" + proxyHost.getPort());

                                              try {
                                                  return proxyObject.proxy(agentId, message);
                                              } catch (IOException e) {
                                                  throw new RuntimeException(e);
                                              }
                                          },
                                          executor)
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
                                     invocationManager.onClientException(txId, ex);
                                 }
                             });
        }

        private byte[] toByteArray(ServiceRequestMessageOut outMessage) throws IOException {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024)) {
                CodedOutputStream stream = CodedOutputStream.newInstance(outputStream);
                outMessage.encode(stream);
                stream.flush();
                return outputStream.toByteArray();
            }
        }
    }
}
