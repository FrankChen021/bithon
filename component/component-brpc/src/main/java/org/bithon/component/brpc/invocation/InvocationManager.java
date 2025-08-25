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
import org.bithon.component.brpc.StreamCancellation;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.brpc.channel.IBrpcChannel;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.exception.CallerSideException;
import org.bithon.component.brpc.exception.ChannelException;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.message.ExceptionMessage;
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.brpc.message.in.ServiceResponseMessageIn;
import org.bithon.component.brpc.message.in.ServiceStreamingDataMessageIn;
import org.bithon.component.brpc.message.in.ServiceStreamingEndMessageIn;
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;
import org.bithon.component.commons.logging.LoggerFactory;
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

            long txId = transactionId.incrementAndGet();
            ServiceRequestMessageOut serviceMessageOut = ServiceRequestMessageOut.builder()
                                                                                 .messageType(ServiceMessageType.CLIENT_STREAMING_REQUEST)
                                                                                 .serviceName(serviceRegistryItem.getServiceName())
                                                                                 .methodName(serviceRegistryItem.getMethodName())
                                                                                 .transactionId(txId)
                                                                                 .serializer(serviceRegistryItem.getSerializer())
                                                                                 .applicationName(invokerName)
                                                                                 .headers(headers)
                                                                                 .args(requestArgs)
                                                                                 .build();

            // Create cancellation object for client control
            ClientStreamingCancellation cancellation = new ClientStreamingCancellation(txId, this);
            
            // Inject the cancellation object into the StreamResponse
            streamResponse.setStreamCancellation(cancellation);

            invokeImpl(channel,
                       serviceMessageOut,
                       null,
                       serviceRegistryItem.getStreamingDataType(),
                       streamResponse,
                       cancellation,
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
    public <T> T invokeRpc(IBrpcChannel channel,
                           ServiceRequestMessageOut serviceRequest,
                           long timeoutMillisecond) throws Throwable {
        //noinspection unchecked
        return (T) invokeImpl(channel, serviceRequest, null, null, null, null, timeoutMillisecond);
    }

    public void invokeStreamingRpc(IBrpcChannel channel,
                                   ServiceRequestMessageOut streamingRequest,
                                   Type streamingDataType,
                                   StreamResponse<?> response,
                                   long timeoutMilliseconds) throws Throwable {
        // Create cancellation object for client control
        ClientStreamingCancellation cancellation = new ClientStreamingCancellation(streamingRequest.getTransactionId(), this);
        
        // Inject the cancellation object into the StreamResponse
        response.setStreamCancellation(cancellation);
        
        invokeImpl(channel, streamingRequest, null, streamingDataType, response, cancellation, timeoutMilliseconds);
    }


    private Object invokeImpl(IBrpcChannel channel,
                              ServiceRequestMessageOut serviceRequest,
                              Type returnObjectType,
                              Type streamingDataType,
                              StreamResponse<?> streamResponse,
                              StreamCancellation cancellation,
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
                                                      (StreamResponse<Object>) streamResponse,
                                                      cancellation,
                                                      channel);
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

                    // Success, clear exception
                    exception = null;
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
            return inflightRequest.returnObject;
        } else {
            throw new CallerSideException("Failed to invoke %s#%s at [%s] due to timeout",
                                          serviceRequest.getServiceName(),
                                          serviceRequest.getMethodName(),
                                          channel.getRemoteAddress());
        }
    }

    /**
     * Handle response from server
     */
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
     * Handle streaming data from server
     */
    public void handleStreamingData(ServiceStreamingDataMessageIn dataMessage) {
        long txId = dataMessage.getTransactionId();
        InflightRequest inflightRequest = inflightRequests.get(txId);
        if (inflightRequest == null || !inflightRequest.isStreaming()) {
            return;
        }

        try {
            Object data = inflightRequest.streamingDataType == null ? dataMessage.getRawData() : dataMessage.getData(inflightRequest.streamingDataType);

            // Check if client wants to cancel
            if (inflightRequest.cancellation != null && inflightRequest.cancellation.isCancelled()) {
                // Remove from our local tracking and send cancel message
                cancelStreaming(txId);
            } else {
                // Pass the data to the client
                //noinspection DataFlowIssue
                inflightRequest.streamResponse.onNext(data);
            }
        } catch (Exception e) {
            inflightRequests.remove(txId);

            //noinspection DataFlowIssue
            inflightRequest.streamResponse.onException(e);
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
     *
     * @param txId The transaction ID of the streaming request to cancel
     */
    public void cancelStreaming(long txId) {
        InflightRequest inflightRequest = inflightRequests.remove(txId);
        if (inflightRequest == null || !inflightRequest.isStreaming()) {
            return;
        }

        try {
            IBrpcChannel channel = inflightRequest.channel;
            if (channel != null && channel.isActive()) {
                // Send cancel message to server
                ServiceRequestMessageOut cancelRequest = ServiceRequestMessageOut.builder()
                                                                                 .messageType(ServiceMessageType.CLIENT_STREAMING_CANCEL)
                                                                                 .transactionId(txId)
                                                                                 .applicationName("brpc-client")
                                                                                 .serviceName(inflightRequest.serviceName)
                                                                                 .methodName(inflightRequest.methodName)
                                                                                 .build();

                channel.writeAsync(cancelRequest);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(InvocationManager.class)
                         .warn("Failed to send streaming cancel message for txId: " + txId, e);
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
        ExceptionMessage e = new ExceptionMessage(ServiceInvocationException.class.getName(), "Channel is closed before request completes");

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
    
    /**
     * Get an inflight request by transaction ID.
     * This is used by HTTP proxy to access the StreamResponse for cancellation.
     * 
     * @param txId The transaction ID to look up
     * @return The InflightRequest, or null if not found
     */
    public InflightRequest getInflightRequest(long txId) {
        return inflightRequests.get(txId);
    }

    public static class InflightRequest {
        public final String serviceName;
        public final String methodName;
        public final long requestAt;

        // Channel used for this request - needed for cancellation
        public final IBrpcChannel channel;

        // For streaming calls
        public final Type streamingDataType;
        public final StreamResponse<Object> streamResponse;
        public final StreamCancellation cancellation;

        // For non-streaming calls
        public final Type returnObjectType;
        public volatile long responseAt;
        public volatile Object returnObject;
        public volatile ExceptionMessage exception;

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
            this.cancellation = null;
            this.channel = null;
        }

        /**
         * Constructor for streaming calls
         */
        private InflightRequest(String serviceName,
                                String methodName,
                                Type streamingDataType,
                                StreamResponse<Object> streamResponse,
                                StreamCancellation cancellation) {
            this.serviceName = serviceName;
            this.methodName = methodName;
            this.requestAt = System.currentTimeMillis();
            this.returnObjectType = null;
            this.streamingDataType = streamingDataType;
            this.streamResponse = streamResponse;
            this.cancellation = cancellation;
            this.channel = null;
        }

        /**
         * Constructor for streaming calls with channel reference
         */
        private InflightRequest(String serviceName,
                                String methodName,
                                Type streamingDataType,
                                StreamResponse<Object> streamResponse,
                                StreamCancellation cancellation,
                                IBrpcChannel channel) {
            this.serviceName = serviceName;
            this.methodName = methodName;
            this.requestAt = System.currentTimeMillis();
            this.returnObjectType = null;
            this.streamingDataType = streamingDataType;
            this.streamResponse = streamResponse;
            this.cancellation = cancellation;
            this.channel = channel;
        }

        boolean isStreaming() {
            return this.streamResponse != null;
        }
    }
    
    /**
     * Client-side streaming cancellation implementation that sends cancellation messages to server
     */
    static class ClientStreamingCancellation implements StreamCancellation {
        private final long txId;
        private final InvocationManager invocationManager;
        private volatile boolean cancelled = false;
        
        ClientStreamingCancellation(long txId, InvocationManager invocationManager) {
            this.txId = txId;
            this.invocationManager = invocationManager;
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                // Send immediate cancellation message to server
                invocationManager.cancelStreaming(txId);
            }
        }
        
        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }
}
