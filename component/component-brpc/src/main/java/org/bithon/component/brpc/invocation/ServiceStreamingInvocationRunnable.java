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

import org.bithon.component.brpc.ServiceRegistry;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.brpc.exception.BadRequestException;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.ServiceNotFoundException;
import org.bithon.component.brpc.message.in.ServiceRequestMessageIn;
import org.bithon.component.brpc.message.out.ServiceStreamingDataMessageOut;
import org.bithon.component.brpc.message.out.ServiceStreamingEndMessageOut;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.io.netty.channel.Channel;
import org.bithon.shaded.io.netty.channel.ChannelId;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Runnable for handling streaming RPC invocations on the server side.
 * This class manages the lifecycle of streaming responses and handles
 * bidirectional communication between client and server.
 *
 * @author frankchen
 */
public class ServiceStreamingInvocationRunnable implements Runnable {

    /**
     * Map to track active streaming requests by transaction ID
     */
    private static final Map<Long, StreamingContext> ACTIVE_STREAMS = new ConcurrentHashMap<>();

    private final Channel channel;
    private final long txId;
    private final ServiceRegistry.ServiceInvoker serviceInvoker;
    private final Object[] args;

    public ServiceStreamingInvocationRunnable(Channel channel,
                                              long txId,
                                              ServiceRegistry.ServiceInvoker serviceInvoker,
                                              Object[] args) {
        this.channel = channel;
        this.txId = txId;
        this.serviceInvoker = serviceInvoker;
        this.args = args;
    }

    @Override
    public void run() {
        try {
            // Check if channel is active before starting
            if (!channel.isActive()) {
                throw new ServiceInvocationException("Channel is not active for streaming request");
            }

            // Create a StreamResponse implementation for the server that sends data back to client
            StreamResponse<Object> streamResponse = new StreamResponse<Object>() {
                @Override
                public void onNext(Object data) {
                    try {
                        // Check channel status before sending
                        if (!channel.isActive() || !channel.isWritable()) {
                            // Channel is closed - mark as cancelled and return instead of throwing
                            StreamingContext ctx = ACTIVE_STREAMS.get(txId);
                            if (ctx != null) {
                                ctx.cancel();
                            }
                            return;
                        }

                        LoggerFactory.getLogger(ServiceStreamingInvocationRunnable.class)
                                     .info("Sending streaming data for txId: {}, data: {}", txId, data);
                        new ServiceStreamingDataMessageOut(txId, data, serviceInvoker.getMetadata().getSerializer())
                            .send(channel);
                    } catch (Exception e) {
                        LoggerFactory.getLogger(ServiceStreamingInvocationRunnable.class)
                                     .warn("Failed to send streaming data, marking as cancelled", e);
                        // Mark as cancelled instead of calling onError to avoid infinite recursion
                        StreamingContext ctx = ACTIVE_STREAMS.get(txId);
                        if (ctx != null) {
                            ctx.cancel();
                        }
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    try {
                        // Only try to send error if channel is still active
                        if (channel.isActive()) {
                            new ServiceStreamingEndMessageOut(txId, throwable).send(channel);
                        }
                    } catch (Exception e) {
                        LoggerFactory.getLogger(ServiceStreamingInvocationRunnable.class)
                                     .error("Failed to send streaming error", e);
                    } finally {
                        // Mark as completed and clean up
                        StreamingContext ctx = ACTIVE_STREAMS.remove(txId);
                        if (ctx != null) {
                            ctx.markCompleted();
                        }
                    }
                }

                @Override
                public void onComplete() {
                    try {
                        // Only try to send completion if channel is still active
                        if (channel.isActive()) {
                            new ServiceStreamingEndMessageOut(txId).send(channel);
                        }
                    } catch (Exception e) {
                        LoggerFactory.getLogger(ServiceStreamingInvocationRunnable.class)
                                     .error("Failed to send streaming completion", e);
                    } finally {
                        // Mark as completed and clean up
                        StreamingContext ctx = ACTIVE_STREAMS.remove(txId);
                        if (ctx != null) {
                            ctx.markCompleted();
                        }
                    }
                }

                @Override
                public boolean isCancelled() {
                    StreamingContext context = ACTIVE_STREAMS.get(txId);
                    return context != null && (context.isCancelled() || !channel.isActive());
                }
            };

            // Store streaming context with channel monitoring
            StreamingContext context = new StreamingContext(channel.id());
            ACTIVE_STREAMS.put(txId, context);

            // Set up channel closure handler
            channel.closeFuture()
                   .addListener(future -> {
                       StreamingContext ctx = ACTIVE_STREAMS.remove(txId);
                       if (ctx != null) {
                           if (ctx.isCompleted()) {
                               // Normal closure after streaming completed
                               LoggerFactory.getLogger(ServiceStreamingInvocationRunnable.class)
                                            .debug("Channel closed after streaming completed, txId: {}", txId);
                           } else {
                               // Unexpected closure during active streaming
                               LoggerFactory.getLogger(ServiceStreamingInvocationRunnable.class)
                                            .warn("Channel closed during active streaming, txId: {}", txId);
                               // Notify the service that the channel is closed via cancellation
                               ctx.cancel();
                           }
                       }
                   });

            // Add StreamResponse as the last argument
            Object[] streamingArgs = new Object[args.length + 1];
            System.arraycopy(args, 0, streamingArgs, 0, args.length);
            streamingArgs[args.length] = streamResponse;

            try {
                serviceInvoker.invoke(streamingArgs);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("[Client=%s] Bad Request: Service[%s#%s] exception: Illegal argument",
                                              channel.remoteAddress().toString(),
                                              serviceInvoker.getMetadata().getServiceName(),
                                              serviceInvoker.getMetadata().getMethodName());
            } catch (IllegalAccessException e) {
                throw new ServiceInvocationException("[Client=%s] Service[%s#%s] exception: %s",
                                                     channel.remoteAddress().toString(),
                                                     serviceInvoker.getMetadata().getServiceName(),
                                                     serviceInvoker.getMetadata().getMethodName(),
                                                     e.getMessage());
            } catch (InvocationTargetException e) {
                throw new ServiceInvocationException(e.getTargetException(),
                                                     "[Client=%s] Service[%s#%s] invocation exception",
                                                     channel.remoteAddress().toString(),
                                                     serviceInvoker.getMetadata().getServiceName(),
                                                     serviceInvoker.getMetadata().getMethodName());
            }

        } catch (ServiceInvocationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LoggerFactory.getLogger(ServiceStreamingInvocationRunnable.class)
                         .error(StringUtils.format("[Client=%s] Streaming Service Invocation on %s#%s",
                                                   channel.remoteAddress().toString(),
                                                   serviceInvoker.getMetadata().getServiceName(),
                                                   serviceInvoker.getMetadata().getMethodName()),
                                cause);

            try {
                // Only try to send error if channel is still active
                if (channel.isActive()) {
                    new ServiceStreamingEndMessageOut(txId, cause).send(channel);
                }
            } catch (Exception sendException) {
                LoggerFactory.getLogger(ServiceStreamingInvocationRunnable.class)
                             .error("Failed to send streaming error response", sendException);
            } finally {
                ACTIVE_STREAMS.remove(txId);
            }
        }
    }

    public static void execute(ServiceRegistry serviceRegistry,
                               Channel channel,
                               ServiceRequestMessageIn serviceRequest,
                               Executor executor) {
        try {
            if (serviceRequest.getServiceName() == null) {
                throw new BadRequestException("[Client=%s] serviceName is null", channel.remoteAddress().toString());
            }

            if (serviceRequest.getMethodName() == null) {
                throw new BadRequestException("[Client=%s] methodName is null", channel.remoteAddress().toString());
            }

            if (!serviceRegistry.contains(serviceRequest.getServiceName())) {
                throw new ServiceNotFoundException(serviceRequest.getServiceName());
            }

            ServiceRegistry.ServiceInvoker serviceInvoker = serviceRegistry.findServiceInvoker(serviceRequest.getServiceName(),
                                                                                               serviceRequest.getMethodName());
            if (serviceInvoker == null) {
                throw new ServiceNotFoundException(serviceRequest.getServiceName() + "#" + serviceRequest.getMethodName());
            }

            try {
                // Read args outside the thread pool
                // For streaming methods, we exclude the last parameter (StreamResponse) from the request
                Type[] parameterTypes = serviceInvoker.getParameterTypes();
                Type[] requestParameterTypes = new Type[parameterTypes.length - 1];
                System.arraycopy(parameterTypes, 0, requestParameterTypes, 0, parameterTypes.length - 1);

                Object[] args = serviceRequest.readArgs(requestParameterTypes);

                executor.execute(new ServiceStreamingInvocationRunnable(channel,
                                                                        serviceRequest.getTransactionId(),
                                                                        serviceInvoker,
                                                                        args));
            } catch (IOException e) {
                throw new BadRequestException("[Client=%s] Bad Request: Service[%s#%s]: %s",
                                              channel.remoteAddress().toString(),
                                              serviceRequest.getServiceName(),
                                              serviceRequest.getMethodName(),
                                              e.getMessage());
            }
        } catch (ServiceInvocationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            boolean isClientSideException = e instanceof BadRequestException || e instanceof ServiceNotFoundException;
            if (!isClientSideException) {
                LoggerFactory.getLogger(ServiceStreamingInvocationRunnable.class)
                             .error(StringUtils.format("[Client=%s] Streaming Service Invocation on %s#%s",
                                                       channel.remoteAddress().toString(),
                                                       serviceRequest.getServiceName(),
                                                       serviceRequest.getMethodName()),
                                    cause);
            }

            try {
                new ServiceStreamingEndMessageOut(serviceRequest.getTransactionId(), cause).send(channel);
            } catch (Exception sendException) {
                LoggerFactory.getLogger(ServiceStreamingInvocationRunnable.class)
                             .error("Failed to send streaming error response", sendException);
            }
        }
    }

    /**
     * Cancel a streaming request
     */
    public static void cancelStreaming(long txId) {
        StreamingContext context = ACTIVE_STREAMS.remove(txId);
        if (context != null) {
            context.cancel();
        }
    }

    /**
     * Clean up all streaming contexts associated with a specific channel
     * This is called when a channel becomes inactive
     */
    public static void cleanupForChannel(Channel channel) {
        if (channel != null) {
            ChannelId channelId = channel.id();
            ACTIVE_STREAMS.entrySet()
                          .removeIf(entry -> {
                              StreamingContext context = entry.getValue();
                              if (channelId.equals(context.channelId)) {
                                  LoggerFactory.getLogger(ServiceStreamingInvocationRunnable.class)
                                               .info("Cleaning up streaming context for closed channel, txId: {}, channelId: {}",
                                                     entry.getKey(), channelId);
                                  context.cancel();
                                  return true;
                              }
                              return false;
                          });
        }
    }

    /**
     * Context for managing streaming state
     */
    private static class StreamingContext {
        private final ChannelId channelId;
        private volatile boolean cancelled = false;
        private volatile boolean completed = false;

        public StreamingContext(ChannelId channelId) {
            this.channelId = channelId;
        }

        public void cancel() {
            this.cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void markCompleted() {
            this.completed = true;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
} 
