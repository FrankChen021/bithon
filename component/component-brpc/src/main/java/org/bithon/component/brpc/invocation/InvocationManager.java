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
import org.bithon.component.brpc.message.in.ServiceResponseMessageIn;
import org.bithon.component.brpc.message.in.ServiceStreamingDataMessageIn;
import org.bithon.component.brpc.message.in.ServiceStreamingEndMessageIn;
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;
import org.bithon.component.brpc.message.out.ServiceStreamingRequestMessageOut;
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

    /**
     * key is transaction id of streaming requests
     */
    private final Map<Long, StreamingRequest> streamingRequests = new ConcurrentHashMap<>();

    private final Map<Method, ServiceRegistryItem> serviceRegistryItems = new ConcurrentHashMap<>();

    public Object invoke(String invokerName,
                         Headers headers,
                         IBrpcChannel channel,
                         long timeoutMillisecond,
                         Method method,
                         Object[] args) throws Throwable {

        ServiceRegistryItem serviceRegistryItem = serviceRegistryItems.computeIfAbsent(method, ServiceRegistryItem::create);

        // Handle streaming methods
        if (serviceRegistryItem.isStreaming()) {
            return invokeStreaming(invokerName, headers, channel, serviceRegistryItem, args);
        }

        ServiceRequestMessageOut serviceRequest = ServiceRequestMessageOut.builder()
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

        return invoke(channel,
                      serviceRequest,
                      method.getGenericReturnType(),
                      timeoutMillisecond);
    }

    @SuppressWarnings("unchecked")
    private Object invokeStreaming(String invokerName,
                                   Headers headers,
                                   IBrpcChannel channel,
                                   ServiceRegistryItem serviceRegistryItem,
                                   Object[] args) throws Throwable {
        // Extract StreamResponse from the last argument
        StreamResponse<Object> streamResponse = (StreamResponse<Object>) args[args.length - 1];

        // Create args array without the StreamResponse
        Object[] requestArgs = new Object[args.length - 1];
        System.arraycopy(args, 0, requestArgs, 0, args.length - 1);

        long txId = transactionId.incrementAndGet();
        // Send streaming request
        channel.connect();
        checkChannelStatus(channel, serviceRegistryItem.getServiceName(), serviceRegistryItem.getMethodName());

        // Store streaming request
        StreamingRequest streamingRequest = new StreamingRequest(
            serviceRegistryItem.getServiceName(),
            serviceRegistryItem.getMethodName(),
            serviceRegistryItem.getStreamingDataType(),
            streamResponse
        );
        streamingRequests.put(txId, streamingRequest);

        ServiceRequestMessageOut serviceMessage = ServiceStreamingRequestMessageOut.builder()
                                                                                   .serviceName(serviceRegistryItem.getServiceName())
                                                                                   .methodName(serviceRegistryItem.getMethodName())
                                                                                   .transactionId(txId)
                                                                                   .serializer(serviceRegistryItem.getSerializer())
                                                                                   .applicationName(invokerName)
                                                                                   .headers(headers)
                                                                                   .args(requestArgs)
                                                                                   .build();

        Exception exception = null;
        for (int i = 0; i < 3; i++) {
            try {
                channel.writeAsync(serviceMessage);

                // Streaming methods return void
                return null;
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

        streamingRequests.remove(txId);

        // If we reach here, it means all attempts to send the streaming request failed
        return exception;
    }

    private void checkChannelStatus(IBrpcChannel channel, String serviceName, String methodName) throws CallerSideException {
        EndPoint remoteEndpoint = channel.getRemoteAddress();
        if (remoteEndpoint == null) {
            throw new CallerSideException("Failed to invoke %s#%s due to channel is empty", serviceName, methodName);
        }

        if (!channel.isActive()) {
            throw new CallerSideException("Failed to invoke %s#%s at [%s] due to channel is not active", serviceName, methodName, remoteEndpoint);
        }

        // Wait for the channel to be writable
        long deadline = System.currentTimeMillis() + 1000;
        while (!channel.isWritable()) {
            if (System.currentTimeMillis() > deadline) {
                throw new CallerSideException("Failed to invoke %s#%s at [%s] due to channel is not writable", serviceName, methodName, remoteEndpoint);
            }
            try {
                // A short sleep to prevent busy-waiting, allowing the event loop to process I/O
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
                break;
            }
        }
    }

    /**
     * Invoke a remote service method and returns the raw byte-stream response.
     * This is used for proxy.
     */
    public byte[] invoke(IBrpcChannel channel,
                         ServiceRequestMessageOut serviceRequest,
                         long timeoutMillisecond) throws Throwable {
        return (byte[]) invoke(channel, serviceRequest, null, timeoutMillisecond);
    }

    private Object invoke(IBrpcChannel channel,
                          ServiceRequestMessageOut serviceRequest,
                          Type returnObjectType,
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
            inflightRequest = new InflightRequest(serviceRequest.getServiceName(),
                                                  serviceRequest.getMethodName(),
                                                  returnObjectType);
            this.inflightRequests.put(serviceRequest.getTransactionId(), inflightRequest);
        }

        Exception exception = null;
        for (int i = 0; i < 3; i++) {
            try {
                channel.writeAsync(serviceRequest);
                break;
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
            inflightRequests.remove(serviceRequest.getTransactionId());
            throw exception;
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
        return null;
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
        StreamingRequest streamingRequest = streamingRequests.get(txId);
        if (streamingRequest == null) {
            return;
        }

        try {
            Object data = dataMessage.getData(streamingRequest.dataType);
            streamingRequest.streamResponse.onNext(data);

            // Check if client wants to cancel
            if (streamingRequest.streamResponse.isCancelled()) {
                cancelStreaming(txId);
            }
        } catch (Exception e) {
            streamingRequest.streamResponse.onException(e);
            streamingRequests.remove(txId);
        }
    }

    /**
     * Handle streaming end message
     */
    public void handleStreamingEnd(ServiceStreamingEndMessageIn endMessage) {
        long txId = endMessage.getTransactionId();
        StreamingRequest streamingRequest = streamingRequests.remove(txId);
        if (streamingRequest == null) {
            return;
        }

        if (endMessage.hasException()) {
            streamingRequest.streamResponse.onException(endMessage.getException());
        } else {
            streamingRequest.streamResponse.onComplete();
        }
    }

    /**
     * Cancel a streaming request
     */
    public void cancelStreaming(long txId) {
        StreamingRequest streamingRequest = streamingRequests.remove(txId);
        if (streamingRequest != null) {
            // Send cancel message to server
            // This would need access to the channel, which we'd need to store in StreamingRequest
            // For now, just remove from local tracking
        }
    }

    public void handleException(long txId, Throwable e) {
        InflightRequest inflightRequest = inflightRequests.remove(txId);
        if (inflightRequest != null) {
            inflightRequest.exception = new ExceptionMessage(CallerSideException.class.getName(), e.getMessage());
            synchronized (inflightRequest) {
                inflightRequest.responseAt = System.currentTimeMillis();
                inflightRequest.notifyAll();
            }
        }

        // Also handle streaming requests
        StreamingRequest streamingRequest = streamingRequests.remove(txId);
        if (streamingRequest != null) {
            streamingRequest.streamResponse.onException(e);
        }
    }

    /**
     * Handle channel closure - clean up all active requests and streams
     */
    public void handleChannelClosure() {
        ExceptionMessage e = new ExceptionMessage(ServiceInvocationException.class.getName(), "Channel closed");

        // Clean up regular requests
        inflightRequests.values()
                        .removeIf(request -> {
                            request.exception = e;
                            synchronized (request) {
                                request.responseAt = System.currentTimeMillis();
                                request.notifyAll();
                            }
                            return true;
                        });

        // Clean up streaming requests
        Exception ex = e.toException();
        streamingRequests.values()
                         .removeIf(streamingRequest -> {
                             try {
                                 streamingRequest.streamResponse.onException(ex);
                             } catch (Exception ignored) {
                             }
                             return true;
                         });
    }

    /**
     * Check if a streaming request is still active (channel is open)
     */
    public boolean isStreamingActive(long txId) {
        StreamingRequest streamingRequest = streamingRequests.get(txId);
        return streamingRequest != null;
    }

    /**
     * Clean up inactive streaming requests
     */
    public void cleanupInactiveStreams() {
        streamingRequests.entrySet().removeIf(entry -> {
            StreamingRequest streamingRequest = entry.getValue();
            // Check if the stream should be cleaned up due to timeout or other conditions
            long timeoutMs = 300000; // 5 minutes default timeout
            if (System.currentTimeMillis() - streamingRequest.requestAt > timeoutMs) {
                try {
                    streamingRequest.streamResponse.onException(
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

        /**
         * The type of the return object
         */
        final Type returnObjectType;

        final long requestAt;

        private InflightRequest(String serviceName,
                                String methodName,
                                Type returnObjectType) {
            this.serviceName = serviceName;
            this.methodName = methodName;
            this.returnObjectType = returnObjectType;
            this.requestAt = System.currentTimeMillis();
        }

        volatile long responseAt;
        volatile Object returnObject;
        volatile ExceptionMessage exception;
    }

    static class StreamingRequest {
        final String serviceName;
        final String methodName;
        final Type dataType;
        final StreamResponse<Object> streamResponse;
        final long requestAt;

        private StreamingRequest(String serviceName,
                                 String methodName,
                                 Type dataType,
                                 StreamResponse<Object> streamResponse) {
            this.serviceName = serviceName;
            this.methodName = methodName;
            this.dataType = dataType;
            this.streamResponse = streamResponse;
            this.requestAt = System.currentTimeMillis();
        }
    }
}
