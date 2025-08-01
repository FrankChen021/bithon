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
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bithon.component.brpc.channel.IBrpcChannel;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.exception.ServiceNotFoundException;
import org.bithon.component.brpc.exception.SessionNotFoundException;
import org.bithon.component.brpc.invocation.InvocationManager;
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.brpc.message.in.ServiceResponseMessageIn;
import org.bithon.component.brpc.message.in.ServiceStreamingDataMessageIn;
import org.bithon.component.brpc.message.in.ServiceStreamingEndMessageIn;
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.discovery.client.DiscoveredServiceInstance;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.discovery.client.ErrorResponseDecoder;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.bithon.server.discovery.declaration.controller.IAgentControllerApi;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

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
    public <T> T createBroadcastProxy(Map<String, Object> context,
                                      Class<T> agentServiceDeclaration) {
        // instance is a mandatory parameter
        String application = (String) context.get(IAgentControllerApi.PARAMETER_NAME_APP_NAME);
        Preconditions.checkNotNull(application, "'%s' is not given in the context.", IAgentControllerApi.PARAMETER_NAME_APP_NAME);

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
                                          new AgentServiceBroadcastInvoker(application, instance, context, invocationManager));
    }

    /**
     * @param agentServiceDeclaration The service located at agent side that we want to invoke
     */
    public <T> T createUnicastProxy(Class<T> agentServiceDeclaration,
                                    DiscoveredServiceInstance controller,
                                    String appName,
                                    String instanceName) {
        // Check if given service declaration is correctly declared
        DiscoverableService metadata = IAgentControllerApi.class.getAnnotation(DiscoverableService.class);
        if (metadata == null) {
            throw new RuntimeException(StringUtils.format("Given class [%s] is not marked by annotation [%s].",
                                                          IAgentControllerApi.class.getName(),
                                                          DiscoverableService.class.getSimpleName()));
        }

        String token = "";
        Authentication authentication = SecurityContextHolder.getContext() == null ? null : SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() != null) {
            token = (String) authentication.getCredentials();
        }

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(agentServiceDeclaration.getClassLoader(),
                                          new Class<?>[]{agentServiceDeclaration},
                                          new AgentServiceUnicastInvoker(controller,
                                                                         invocationManager,
                                                                         token,
                                                                         appName,
                                                                         instanceName));
    }

    private class AgentServiceUnicastInvoker implements InvocationHandler {

        private final DiscoveredServiceInstance controller;
        private final InvocationManager brpcInvocationManager;
        private final String targetApplication;
        private final String targetInstance;
        private final String token;

        private AgentServiceUnicastInvoker(DiscoveredServiceInstance controller,
                                           InvocationManager brpcInvocationManager,
                                           String token,
                                           String targetApplication,
                                           String targetInstance) {
            this.controller = controller;
            this.brpcInvocationManager = brpcInvocationManager;
            this.token = token;
            this.targetApplication = targetApplication;
            this.targetInstance = targetInstance;
        }

        @Override
        public Object invoke(Object object,
                             Method agentServiceMethod,
                             Object[] args) {
            //
            // Invoke a synchronous request-response agent service via proxy at controller side
            //
            try {
                return brpcInvocationManager.invoke("bithon-webservice",
                                                    Headers.EMPTY,
                                                    new BrpcChannelOverHttp(controller,
                                                                            this.targetApplication,
                                                                            this.targetInstance,
                                                                            this.token),
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
            } catch (ServiceNotFoundException e) {
                throw new HttpMappableException(HttpStatus.NOT_FOUND.value(),
                                                "Can't find service [%s] on target application [appName = %s, instance = %s]. You may need to upgrade the agent of the target application.",
                                                e.getServiceName(),
                                                targetApplication,
                                                targetInstance);
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Broadcast invocation of the agent service on target proxy servers
     * and the proxy server is responsible for invoking agent service on given agent
     */
    private class AgentServiceBroadcastInvoker implements InvocationHandler {

        private final InvocationManager brpcInvocationManager;
        private final Map<String, Object> context;
        private final String targetApplication;
        private final String targetInstance;

        private AgentServiceBroadcastInvoker(String targetApplication,
                                             String targetInstance,
                                             Map<String, Object> context,
                                             InvocationManager brpcInvocationManager) {
            this.brpcInvocationManager = brpcInvocationManager;
            this.targetApplication = targetApplication;
            this.targetInstance = targetInstance;

            // Make sure the context is modifiable because we're going to add token into the context
            this.context = new TreeMap<>(context);
        }

        @Override
        public Object invoke(Object object,
                             Method agentServiceMethod,
                             Object[] args) {
            // Since the real invocation is issued from a dedicated thread-pool,
            // to make sure the task in that thread pool can access the security context, we have to explicitly
            Authentication authentication = SecurityContextHolder.getContext() == null ? null : SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() != null) {
                context.put("X-Bithon-Token", authentication.getCredentials());
            }

            //
            // Find given agent instance on each controller
            //
            List<DiscoveredServiceInstance> connectedController = Collections.synchronizedList(new ArrayList<>());
            List<DiscoveredServiceInstance> controllerList = discoveryServiceInvoker.getInstanceList(IAgentControllerApi.class);
            if (CollectionUtils.isEmpty(controllerList)) {
                throw new HttpMappableException(HttpStatus.NOT_FOUND.value(),
                                                "No controller is found in the service discovery registry");
            }

            CountDownLatch countDownLatch = new CountDownLatch(controllerList.size());
            for (DiscoveredServiceInstance controller : controllerList) {
                discoveryServiceInvoker.getExecutor()
                                       .submit(() -> discoveryServiceInvoker.createUnicastApi(IAgentControllerApi.class, () -> controller)
                                                                            .getAgentInstanceList(targetApplication, targetInstance))
                                       .thenAccept((returning) -> {
                                           List<Object[]> applicationInstanceList = returning.stream()
                                                                                             .map(IAgentControllerApi.AgentInstanceRecord::toObjectArray)
                                                                                             .toList();
                                           if (CollectionUtils.isNotEmpty(applicationInstanceList)) {
                                               connectedController.add(controller);
                                           }
                                       })
                                       .whenComplete((ret, ex) -> countDownLatch.countDown());
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (connectedController.isEmpty()) {
                throw new HttpMappableException(HttpStatus.NOT_FOUND.value(),
                                                "Can't find target application instance [appName = %s, instance = %s] on any controller",
                                                targetApplication,
                                                targetInstance);
            }

            //
            // Invoke agent service via a controller
            //
            try {
                return brpcInvocationManager.invoke("bithon-webservice",
                                                    Headers.EMPTY,
                                                    new BrpcChannelOverHttp(connectedController.get(0),
                                                                            this.targetApplication,
                                                                            this.targetInstance,
                                                                            context.getOrDefault("X-Bithon-Token", "").toString()),
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
            } catch (ServiceNotFoundException e) {
                throw new HttpMappableException(HttpStatus.NOT_FOUND.value(),
                                                "Can't find service [%s] on target application [appName = %s, instance = %s]. You may need to upgrade the agent of the target application.",
                                                e.getServiceName(),
                                                targetApplication,
                                                targetInstance);
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    class BrpcChannelOverHttp implements IBrpcChannel {
        private final DiscoveredServiceInstance controller;
        private final String targetApplication;
        private final String targetInstance;
        private final String token;

        public BrpcChannelOverHttp(DiscoveredServiceInstance controller,
                                   String targetApplication,
                                   String targetInstance,
                                   String token) {
            this.controller = controller;
            this.targetApplication = targetApplication;
            this.targetInstance = targetInstance;
            this.token = token;
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
            return new EndPoint(controller.getHost(), controller.getPort());
        }

        @Override
        public void writeAsync(ServiceRequestMessageOut serviceRequest) throws IOException {
            if (serviceRequest.getMessageType() == ServiceMessageType.CLIENT_STREAMING_REQUEST) {
                sendStreamingRpc(serviceRequest);
            } else {
                sendRequestResponseRpc(serviceRequest);
            }
        }

        private void sendRequestResponseRpc(ServiceRequestMessageOut serviceRequest) throws IOException {
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
                                                                                  .target(IAgentControllerApi.class, controller.getURL());

                                              try {
                                                  return proxyApi.callAgentService(token,
                                                                                   targetApplication,
                                                                                   targetInstance,
                                                                                   30_000,
                                                                                   message);
                                              } catch (IOException e) {
                                                  throw new RuntimeException(e);
                                              }
                                          },
                                          discoveryServiceInvoker.getExecutor())
                             .thenAccept((responseBytes) -> {
                                 try {
                                     ServiceResponseMessageIn response = ServiceResponseMessageIn.from(new ByteArrayInputStream(responseBytes));
                                     invocationManager.handleResponse(response);
                                 } catch (IOException e) {
                                     invocationManager.handleException(txId, e);
                                 }
                             })
                             .whenComplete((v, ex) -> {
                                 if (ex != null) {
                                     invocationManager.handleException(txId, ex.getCause() != null ? ex.getCause() : ex);
                                 }
                             });
        }

        private void sendStreamingRpc(ServiceRequestMessageOut serviceRequest) throws IOException {
            final long txId = serviceRequest.getTransactionId();
            final byte[] message = serviceRequest.toByteArray();

            CompletableFuture.runAsync(() -> {
                                 try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                                     URI uri = new URIBuilder(controller.getURL() + "/api/agent/service/proxy/streaming")
                                         .addParameter("appName", targetApplication)
                                         .addParameter("instance", targetInstance)
                                         .addParameter("timeout", "30000")
                                         .build();

                                     HttpPost httpPost = new HttpPost(uri);
                                     httpPost.setEntity(new ByteArrayEntity(message, ContentType.APPLICATION_OCTET_STREAM));
                                     if (StringUtils.hasText(token)) {
                                         httpPost.setHeader("X-Bithon-Token", token);
                                     }

                                     try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                                         int statusCode = response.getStatusLine().getStatusCode();
                                         HttpEntity entity = response.getEntity();
                                         if (entity == null) {
                                             // No HTTP body, check status code for error
                                             if (statusCode < 200 || statusCode >= 300) {
                                                 throw new HttpMappableException(statusCode, "Empty response from server with status: " + statusCode);
                                             }
                                             return; // Or handle as completion if appropriate
                                         }

                                         if (statusCode < 200 || statusCode >= 300) {
                                             String errorBody = EntityUtils.toString(entity);
                                             throw new HttpMappableException(statusCode, errorBody);
                                         }

                                         try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8))) {
                                             String line;
                                             String eventName = null;
                                             while ((line = reader.readLine()) != null) {
                                                 if (line.startsWith("event:")) {
                                                     eventName = line.substring("event:".length()).trim();
                                                 } else if (line.startsWith("data:")) {
                                                     String base64Data = line.substring("data:".length());
                                                     if (eventName == null) {
                                                         continue;
                                                     }
                                                     byte[] decoded = Base64.getDecoder().decode(base64Data);
                                                     if ("complete".equals(eventName)) {
                                                         invocationManager.handleStreamingEnd(ServiceStreamingEndMessageIn.from(decoded));
                                                     } else if ("error".equals(eventName)) {
                                                         invocationManager.handleStreamingEnd(ServiceStreamingEndMessageIn.from(decoded));
                                                     } else {
                                                         invocationManager.handleStreamingData(ServiceStreamingDataMessageIn.from(decoded));
                                                     }
                                                 }
                                             }
                                         }
                                     }
                                 } catch (IOException | URISyntaxException e) {
                                     invocationManager.handleException(txId, e);
                                 }
                             }, discoveryServiceInvoker.getExecutor())
                             .whenComplete((v, ex) -> {
                                 if (ex != null) {
                                     invocationManager.handleException(txId, ex.getCause() != null ? ex.getCause() : ex);
                                 }
                             });
        }
    }
}
