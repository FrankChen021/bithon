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

package org.bithon.component.brpc.invocation;

import org.bithon.component.brpc.ServiceRegistryItem;
import org.bithon.component.brpc.channel.IChannelWriter;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.exception.CalleeSideException;
import org.bithon.component.brpc.exception.CallerSideException;
import org.bithon.component.brpc.exception.ChannelException;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.TimeoutException;
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.brpc.message.in.ServiceResponseMessageIn;
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;
import org.bithon.shaded.io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manage inflight requests from a service client to a service provider
 * <p>
 * Note: the concept 'client' here is a relative concept.
 * It could be a network client, which connects to an RPC server,
 * it could also be an RPC server which calls service provided by a network client.
 *
 * @author frankchen
 */
public class InvocationManager {

    private final AtomicLong transactionId = new AtomicLong(21515);

    /**
     * key is transaction id of the request
     */
    private final Map<Long, InflightRequest> inflightRequests = new ConcurrentHashMap<>();

    private final Map<Method, ServiceRegistryItem> serviceRegistryItems = new ConcurrentHashMap<>();

    public Object invoke(String appName,
                         Headers headers,
                         IChannelWriter channelWriter,
                         long timeoutMillisecond,
                         Method method,
                         Object[] args) throws Throwable {

        ServiceRegistryItem serviceRegistryItem = serviceRegistryItems.computeIfAbsent(method, ServiceRegistryItem::create);

        ServiceRequestMessageOut serviceRequest = ServiceRequestMessageOut.builder()
                                                                          .serviceName(serviceRegistryItem.getServiceName())
                                                                          .methodName(serviceRegistryItem.getMethodName())
                                                                          .transactionId(transactionId.incrementAndGet())
                                                                          .serializer(serviceRegistryItem.getSerializer())
                                                                          .isOneway(serviceRegistryItem.isOneway())
                                                                          .messageType(serviceRegistryItem.getMessageType())
                                                                          .applicationName(appName)
                                                                          .headers(headers)
                                                                          .args(args)
                                                                          .build();

        return invoke(channelWriter,
                      serviceRequest,
                      method.getGenericReturnType(),
                      timeoutMillisecond);
    }

    public byte[] invoke(IChannelWriter channelWriter,
                         ServiceRequestMessageOut serviceRequest,
                         long timeoutMillisecond) throws Throwable {
        return (byte[]) invoke(channelWriter, serviceRequest, null, timeoutMillisecond);
    }

    private Object invoke(IChannelWriter channelWriter,
                          ServiceRequestMessageOut serviceRequest,
                          Type returnObjectType,
                          long timeoutMillisecond) throws Throwable {
        //
        // make sure channel has been established
        //
        channelWriter.connect();

        //
        // Check channel status
        //
        EndPoint remoteEndpoint = channelWriter.getRemoteAddress();
        if (remoteEndpoint == null) {
            throw new CallerSideException("Failed to invoke %s#%s due to channel is empty",
                                          serviceRequest.getServiceName(),
                                          serviceRequest.getMethodName());
        }

        if (!channelWriter.isActive()) {
            throw new CallerSideException("Failed to invoke %s#%s at [%s] due to channel is not active",
                                          serviceRequest.getServiceName(),
                                          serviceRequest.getMethodName(),
                                          remoteEndpoint);
        }
        if (!channelWriter.isWritable()) {
            throw new CallerSideException("Failed to invoke %s#%s at [%s] due to channel is not writable",
                                          serviceRequest.getServiceName(),
                                          serviceRequest.getMethodName(),
                                          remoteEndpoint);
        }

        InflightRequest inflightRequest = null;
        if (!serviceRequest.isOneway()) {
            inflightRequest = new InflightRequest(serviceRequest.getServiceName(),
                                                  serviceRequest.getMethodName(),
                                                  returnObjectType,
                                                  System.currentTimeMillis());
            this.inflightRequests.put(serviceRequest.getTransactionId(), inflightRequest);
        }

        for (int i = 0; i < 3; i++) {
            try {
                channelWriter.write(serviceRequest);
                break;
            } catch (ChannelException e) {
                if (i < 2) {
                    channelWriter.connect();
                }
            }
        }

        if (inflightRequest != null) {
            try {
                //noinspection ReassignedVariable,SynchronizationOnLocalVariableOrMethodParameter
                synchronized (inflightRequest) {
                    inflightRequest.wait(timeoutMillisecond);
                }
            } catch (InterruptedException e) {
                inflightRequests.remove(serviceRequest.getTransactionId());
                throw new CallerSideException("Failed to invoke %s#%s at [%s] due to invocation is interrupted",
                                              serviceRequest.getServiceName(),
                                              serviceRequest.getMethodName(),
                                              remoteEndpoint);
            }

            //make sure it has been cleared when timeout
            inflightRequests.remove(serviceRequest.getTransactionId());

            if (inflightRequest.exception != null) {
                throw inflightRequest.exception;
            }

            if (!inflightRequest.responseReceived) {
                throw new TimeoutException(channelWriter.getRemoteAddress().toString(),
                                           serviceRequest.getServiceName(),
                                           serviceRequest.getMethodName(),
                                           timeoutMillisecond);
            }

            return inflightRequest.returnObject;
        }
        return null;
    }

    public void onResponse(ServiceResponseMessageIn response) {
        long txId = response.getTransactionId();
        InflightRequest inflightRequest = inflightRequests.remove(txId);
        if (inflightRequest == null) {
            return;
        }

        if (!StringUtil.isNullOrEmpty(response.getException())) {
            inflightRequest.exception = new CalleeSideException(response.getException());
        } else {
            try {
                inflightRequest.returnObject = inflightRequest.returnObjectType == null ?
                        response.getReturnAsRaw() :
                        response.getReturningAsObject(inflightRequest.returnObjectType);
            } catch (IOException e) {
                inflightRequest.exception = new ServiceInvocationException(e, "Failed to deserialize the received response: %s", e.getMessage());
            }
        }

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (inflightRequest) {
            inflightRequest.responseReceived = true;
            inflightRequest.notify();
        }
    }

    public void onClientException(long txId, Throwable e) {
        InflightRequest inflightRequest = inflightRequests.remove(txId);
        if (inflightRequest == null) {
            return;
        }

        inflightRequest.exception = e;

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (inflightRequest) {
            inflightRequest.responseReceived = true;
            inflightRequest.notify();
        }
    }

    static class InflightRequest {
        final String serviceName;

        final String methodName;

        /**
         * Can be NULL.
         * If NULL, byte-stream is returned.
         */
        final Type returnObjectType;

        final long requestAt;

        public InflightRequest(String serviceName,
                               String methodName,
                               Type returnObjectType,
                               long requestAt) {
            this.serviceName = serviceName;
            this.methodName = methodName;
            this.returnObjectType = returnObjectType;
            this.requestAt = requestAt;
        }

        /**
         * The deserialized response object.
         * If {@link #returnObjectType} is NULL, then this object holds raw byte-stream of the response.
         */
        Object returnObject;

        /**
         * indicate whether this request has response.
         * This is required so that {@link #returnObject} might be null
         */
        boolean responseReceived;
        Throwable exception;
    }
}
