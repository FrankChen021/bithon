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
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.brpc.channel.IBrpcChannel;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.exception.CallerSideException;
import org.bithon.component.brpc.exception.ChannelException;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.TimeoutException;
import org.bithon.component.brpc.message.ExceptionMessage;
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.brpc.message.in.ServiceResponseMessageIn;
import org.bithon.component.brpc.message.in.ServiceStreamingDataMessageIn;
import org.bithon.component.brpc.message.in.ServiceStreamingEndMessageIn;
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;
import org.bithon.component.commons.utils.StringUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manage in-flight requests from a service client to a service provider
 * <p>
 * Note: the concept 'client' here is a relative concept.
 * It could be a network client, which connects to an RPC server,
 * it could also be an RPC server that calls service provided by a network client.
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

    public Object invoke(String invokerName,
                         Headers headers,
                         IBrpcChannel channel,
                         long timeoutMillisecond,
                         Method method,
                         Object[] args) throws Throwable {

        ServiceRegistryItem serviceRegistryItem = serviceRegistryItems.computeIfAbsent(method, ServiceRegistryItem::create);

        if (serviceRegistryItem.isStreaming()) {

            //noinspection unchecked
            StreamResponse<Object> streamResponse = (StreamResponse<Object>) args[args.length - 1];

            // Create args array without the StreamResponse
            Object[] requestArgs = new Object[args.length - 1];
            System.arraycopy(args, 0, requestArgs, 0, args.length - 1);

            ServiceRequestMessageOut serviceMessageOut = ServiceRequestMessageOut.builder()
                                                                                 .messageType(ServiceMessageType.CLIENT_STREAMING_REQUEST)
                                                                                 .serviceName(serviceRegistryItem.getServiceName())
                                                                                 .methodName(serviceRegistryItem.getMethodName())
                                                                                 .transactionId(transactionId.incrementAndGet())
                                                                                 .serializer(serviceRegistryItem.getSerializer())
                                                                                 .applicationName(invokerName)
                                                                                 .headers(headers)
                                                                                 .args(requestArgs)
                                                                                 .build();

            invokeImpl(channel,
                       serviceMessageOut,
                       null,
                       serviceRegistryItem.getStreamingDataType(),
                       streamResponse,
                       timeoutMillisecond);

            return null;
        } else {
            ServiceRequestMessageOut serviceMessageOut = ServiceRequestMessageOut.builder()
                                                                                 .serviceName(serviceRegistryItem.getServiceName())
                                                                                 .methodName(serviceRegistryItem.getMethodName())
                                                                                 .transactionId(transactionId.incrementAndGet())
                                                                                 .serializer(serviceRegistryItem.getSerializer())
                                                                                 .isOneway(serviceRegistryItem.isOneway())
                                                                                 .messageType(serviceRegistryItem.getMessageType())
                                                                                 .applicationName(invokerName)
                                                                                 .headers(headers)
                                                                                 .args(args)
                                                                                 .build();

            return invokeImpl(channel,
                              serviceMessageOut,
                              method.getGenericReturnType(),
                              null,
                              null,
                              timeoutMillisecond);
        }
    }

    private void checkChannelStatus(IBrpcChannel channel, String serviceName, String methodName) throws CallerSideException {
        EndPoint remoteEndpoint = channel.getRemoteAddress();
        if (remoteEndpoint == null) {
            throw new CallerSideException("Failed to invoke %s#%s due to channel is empty", serviceName, methodName);
        }

        if (!channel.isActive()) {
            throw new CallerSideException("Failed to invoke %s#%s at [%s] due to channel is not active", serviceName, methodName, remoteEndpoint);
        }
        if (!channel.isWritable()) {
            throw new CallerSideException("Failed to invoke %s#%s at [%s] due to channel is not writable", serviceName, methodName, remoteEndpoint);
        }
    }

    /**
     * Invoke a remote service method and returns the raw byte-stream response.
     * This is used for proxy.
     */
    public <T> T invokeRpc(IBrpcChannel channel,
                           ServiceRequestMessageOut serviceRequest,
                           long timeoutMillisecond) throws Throwable {
        //noinspection unchecked
        return (T) invokeImpl(channel, serviceRequest, null, null, null, timeoutMillisecond);
    }

    public void invokeStreamingRpc(IBrpcChannel channel,
                                   ServiceRequestMessageOut streamingRequest,
                                   Type streamingDataType,
                                   StreamResponse<?> response,
                                   long timeoutMilliseconds) throws Throwable {
        invokeImpl(channel, streamingRequest, null, streamingDataType, response, timeoutMilliseconds);
    }


    private Object invokeImpl(IBrpcChannel channel,
                              ServiceRequestMessageOut serviceRequest,
                              Type returnObjectType,
                              Type streamingDataType,
                              StreamResponse<?> streamResponse,
                              long timeoutMillisecond) throws Throwable {
        //
        // make sure a channel has been established
        //
        channel.connect();

        //
        // Check channel status
        //
        checkChannelStatus(channel, serviceRequest.getServiceName(), serviceRequest.getMethodName());

        InflightRequest inflightRequest = null;
        if (!serviceRequest.isOneway()) {
            if (serviceRequest.getMessageType() == ServiceMessageType.CLIENT_STREAMING_REQUEST) {
                //noinspection unchecked
                inflightRequest = new InflightRequest(serviceRequest.getServiceName(),
                                                      serviceRequest.getMethodName(),
                                                      streamingDataType,
                                                      (StreamResponse<Object>) streamResponse);
            } else {
                inflightRequest = new InflightRequest(serviceRequest.getServiceName(),
                                                      serviceRequest.getMethodName(),
                                                      returnObjectType);
            }
            this.inflightRequests.put(serviceRequest.getTransactionId(), inflightRequest);
        }

        try {
            Exception exception = null;
            for (int i = 0; i < 3; i++) {
                try {
                    channel.writeAsync(serviceRequest);
                    exception = null; // Success, clear exception
                    break;          // Exit retry loop
                } catch (ChannelException e) {
                    exception = e;
                    if (i < 2) {
                        try {
                            channel.connect();
                        } catch (Exception ex) {
                            exception = ex;
                        }
                    }
                }
            }
            if (exception != null) {
                // All retries failed with ChannelException
                throw exception;
            }
        } catch (Throwable t) {
            inflightRequests.remove(serviceRequest.getTransactionId());
            throw t;
        }

        if (inflightRequest == null
            // For Streaming RPC, the return is always void, and we don't wait for the response as the data/exception will be streamed via the StreamResponse
            || inflightRequest.isStreaming()) {
            return null;
        }

        try {
            //noinspection ReassignedVariable,SynchronizationOnLocalVariableOrMethodParameter
            synchronized (inflightRequest) {
                inflightRequest.wait(timeoutMillisecond);
            }
        } catch (InterruptedException e) {
            // The wait was interrupted, but we still need to clean up before throwing.
            inflightRequests.remove(serviceRequest.getTransactionId());
            throw new CallerSideException("Failed to invoke %s#%s at [%s] due to invocation is interrupted",
                                          serviceRequest.getServiceName(),
                                          serviceRequest.getMethodName(),
                                          channel.getRemoteAddress());
        }

        // Make sure it has been cleared when timeout
        inflightRequests.remove(serviceRequest.getTransactionId());

        if (inflightRequest.exception != null) {
            throw inflightRequest.exception.toException();
        }

        if (inflightRequest.responseAt > 0) {
            // Response has been collected, then return the object.
            // NOTE: The return object might be NULL
            return inflightRequest.returnObject;
        }

        throw new TimeoutException(channel.getRemoteAddress().toString(),
                                   serviceRequest.getServiceName(),
                                   serviceRequest.getMethodName(),
                                   timeoutMillisecond);
    }

    public void handleResponse(ServiceResponseMessageIn response) {
        long txId = response.getTransactionId();
        InflightRequest inflightRequest = inflightRequests.remove(txId);
        if (inflightRequest == null) {
            return;
        }

        if (response.getException() != null) {
            inflightRequest.exception = response.getException();
        } else {
            try {
                inflightRequest.returnObject = inflightRequest.returnObjectType == null ?
                                               response.getReturnAsRaw() :
                                               response.getReturningAsObject(inflightRequest.returnObjectType);
            } catch (IOException e) {
                inflightRequest.exception = new ExceptionMessage(ServiceInvocationException.class.getName(),
                                                                 StringUtils.format("Failed to deserialize the received response: %s", e.getMessage()));
            }
        }

        synchronized (inflightRequest) {
            inflightRequest.responseAt = System.currentTimeMillis();
            inflightRequest.notifyAll();
        }
    }

    /**
     * Handle streaming data message
     */
    public void handleStreamingData(ServiceStreamingDataMessageIn dataMessage) {
        long txId = dataMessage.getTransactionId();
        InflightRequest inflightRequest = inflightRequests.get(txId);
        if (inflightRequest == null || !inflightRequest.isStreaming()) {
            return;
        }

        try {
            Object data = inflightRequest.streamingDataType == null ? dataMessage.getRawData() : dataMessage.getData(inflightRequest.streamingDataType);
            //noinspection DataFlowIssue
            inflightRequest.streamResponse.onNext(data);

            // Check if client wants to cancel
            if (inflightRequest.streamResponse.isCancelled()) {
                cancelStreaming(txId);
            }
        } catch (Exception e) {
            //noinspection DataFlowIssue
            inflightRequest.streamResponse.onException(e);
            inflightRequests.remove(txId);
        }
    }

    /**
     * Handle streaming end message
     */
    public void handleStreamingEnd(ServiceStreamingEndMessageIn endMessage) {
        long txId = endMessage.getTransactionId();
        InflightRequest inflightRequest = inflightRequests.remove(txId);
        if (inflightRequest == null || !inflightRequest.isStreaming()) {
            return;
        }

        if (endMessage.hasException()) {
            //noinspection DataFlowIssue
            inflightRequest.streamResponse.onException(endMessage.getException());
        } else {
            //noinspection DataFlowIssue
            inflightRequest.streamResponse.onComplete();
        }
    }

    /**
     * Cancel a streaming request
     */
    public void cancelStreaming(long txId) {
        InflightRequest inflightRequest = inflightRequests.remove(txId);
        if (inflightRequest != null && inflightRequest.isStreaming()) {
            // Send cancel message to server
            // This would need access to the channel, which we'd need to store in StreamingRequest
            // For now, just remove from local tracking
        }
    }

    public void handleException(long txId, Throwable e) {
        InflightRequest inflightRequest = inflightRequests.remove(txId);
        if (inflightRequest == null) {
            return;
        }

        if (inflightRequest.isStreaming()) {
            //noinspection DataFlowIssue
            inflightRequest.streamResponse.onException(e);
        } else {
            inflightRequest.exception = new ExceptionMessage(CallerSideException.class.getName(), e.getMessage());
            synchronized (inflightRequest) {
                inflightRequest.responseAt = System.currentTimeMillis();
                inflightRequest.notifyAll();
            }
        }
    }

    /**
     * Handle channel closure - clean up all active requests and streams
     */
    public void handleChannelClosure() {
        ExceptionMessage e = new ExceptionMessage(ServiceInvocationException.class.getName(), "Channel closed");

        // Clean up regular requests
        Exception ex = e.toException();
        inflightRequests.values()
                        .removeIf(request -> {
                            if (request.isStreaming()) {
                                try {
                                    //noinspection DataFlowIssue
                                    request.streamResponse.onException(ex);
                                } catch (Exception ignored) {
                                }
                            } else {
                                request.exception = e;
                                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                                synchronized (request) {
                                    request.responseAt = System.currentTimeMillis();
                                    request.notifyAll();
                                }
                            }
                            return true;
                        });
    }

    /**
     * Check if a streaming request is still active (channel is open)
     */
    public boolean isStreamingActive(long txId) {
        InflightRequest inflightRequest = inflightRequests.get(txId);
        return inflightRequest != null && inflightRequest.isStreaming();
    }

    /**
     * Clean up inactive streaming requests
     */
    public void cleanupInactiveStreams() {
        inflightRequests.entrySet().removeIf(entry -> {
            InflightRequest inflightRequest = entry.getValue();
            if (!inflightRequest.isStreaming()) {
                return false;
            }

            // Check if the stream should be cleaned up due to timeout or other conditions
            long timeoutMs = 300000; // 5 minutes default timeout
            if (System.currentTimeMillis() - inflightRequest.requestAt > timeoutMs) {
                try {
                    //noinspection DataFlowIssue
                    inflightRequest.streamResponse.onException(
                        new ServiceInvocationException("Streaming request timed out")
                    );
                } catch (Exception e) {
                    // Ignore callback exceptions
                }
                return true;
            }
            return false;
        });
    }

    static class InflightRequest {
        final String serviceName;
        final String methodName;
        final long requestAt;

        // For streaming calls
        final Type streamingDataType;
        final StreamResponse<Object> streamResponse;

        // For non-streaming calls
        final Type returnObjectType;
        volatile long responseAt;
        volatile Object returnObject;
        volatile ExceptionMessage exception;

        /**
         * Constructor for sync calls
         */
        private InflightRequest(String serviceName,
                                String methodName,
                                Type returnObjectType) {
            this.serviceName = serviceName;
            this.methodName = methodName;
            this.returnObjectType = returnObjectType;
            this.requestAt = System.currentTimeMillis();
            this.streamingDataType = null;
            this.streamResponse = null;
        }

        /**
         * Constructor for streaming calls
         */
        private InflightRequest(String serviceName,
                                String methodName,
                                Type streamingDataType,
                                StreamResponse<Object> streamResponse) {
            this.serviceName = serviceName;
            this.methodName = methodName;
            this.requestAt = System.currentTimeMillis();
            this.returnObjectType = null;
            this.streamingDataType = streamingDataType;
            this.streamResponse = streamResponse;
        }

        boolean isStreaming() {
            return this.streamResponse != null;
        }
    }
}
