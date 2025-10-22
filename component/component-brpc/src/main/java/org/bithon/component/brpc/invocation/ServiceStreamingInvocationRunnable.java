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
import org.bithon.component.brpc.StreamCancellation;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.brpc.exception.BadRequestException;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.message.out.ServiceStreamingDataMessageOut;
import org.bithon.component.brpc.message.out.ServiceStreamingEndMessageOut;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.io.netty.channel.Channel;
import org.bithon.shaded.io.netty.util.AttributeKey;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runnable for handling streaming RPC invocations on the server side.
 * This class manages the lifecycle of streaming responses and handles
 * bidirectional communication between client and server.
 *
 * @author frankchen
 */
public class ServiceStreamingInvocationRunnable implements Runnable {

    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ServiceStreamingInvocationRunnable.class);

    /**
     * AttributeKey for storing streaming contexts in the Channel
     */
    private static final AttributeKey<Map<Long, StreamingContext>> STREAMING_CONTEXTS_KEY = AttributeKey.valueOf("streamingContexts");

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

            // Initialize the contexts map in the channel attributes if needed
            Map<Long, StreamingContext> contexts = channel.attr(STREAMING_CONTEXTS_KEY).get();
            if (contexts == null) {
                contexts = new ConcurrentHashMap<>();
                channel.attr(STREAMING_CONTEXTS_KEY).set(contexts);
            }

            // Create a streaming context first for this transaction
            final StreamingContext streamingContext = new StreamingContext();
            contexts.put(txId, streamingContext);

            // Create a StreamResponse implementation for the server that sends data back to client
            StreamResponse<Object> streamResponse = new StreamResponse<Object>() {
                @Override
                public void onNext(Object data) {
                    try {
                        // Check channel status before sending
                        if (!channel.isActive()) {
                            // Channel is closed - mark as cancelled and return instead of throwing
                            streamingContext.markCancelled();
                            return;
                        }

                        new ServiceStreamingDataMessageOut(txId,
                                                           data,
                                                           serviceInvoker.getMetadata().getSerializer())
                            .send(channel);
                    } catch (Exception e) {
                        LOG.warn("Failed to send streaming data, marking as cancelled", e);

                        // Mark as cancelled instead of calling onError to avoid infinite recursion
                        streamingContext.markCancelled();

                        // Remove the context from the channel
                        removeStreamingContext(channel, txId);
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
                        LOG.error("Failed to send streaming error", e);
                    } finally {
                        streamingContext.markCompleted();

                        // Remove the context from the channel
                        removeStreamingContext(channel, txId);
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
                        LOG.error("Failed to send streaming completion", e);
                    } finally {
                        // Mark as completed and clean up
                        streamingContext.markCompleted();

                        // Remove the context from the channel
                        removeStreamingContext(channel, txId);
                    }
                }

                @Override
                public boolean isCancelled() {
                    return (streamingContext.isCancelled() || !channel.isActive());
                }
            };

            // No need for a channel closure handler here
            // cleanupForChannel will handle it when the channel becomes inactive

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
            LOG.error(StringUtils.format("[Client=%s] Streaming Service Invocation on %s#%s",
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
                LOG.error("Failed to send streaming error response", sendException);
            } finally {
                // Remove the context from the channel
                removeStreamingContext(channel, txId);
            }
        }
    }

    /**
     * Cancel a streaming request
     *
     * @param channel The channel associated with the streaming request
     * @param txId    The transaction ID to cancel
     */
    public static void cancelStreaming(Channel channel, long txId) {
        LOG.debug("Cancelling streaming request, txId: {}", txId);

        StreamingContext ctx = removeStreamingContext(channel, txId);
        if (ctx != null) {
            ctx.markCancelled();
        }
    }

    /**
     * Remove a streaming context from the channel attributes
     *
     * @param channel The channel containing the context
     * @param txId    The transaction ID of the context to remove
     */
    private static StreamingContext removeStreamingContext(Channel channel, long txId) {
        Map<Long, StreamingContext> contexts = channel.attr(STREAMING_CONTEXTS_KEY).get();
        if (contexts != null) {
            return contexts.remove(txId);
        }
        return null;
    }

    /**
     * Clean up all streaming contexts associated with a specific channel
     * This is called when a channel becomes inactive
     */
    public static void cleanupForChannel(Channel channel) {
        if (channel == null) {
            return;
        }

        Map<Long, StreamingContext> contexts = channel.attr(STREAMING_CONTEXTS_KEY).get();
        if (CollectionUtils.isEmpty(contexts)) {
            return;
        }

        // Cancel all active contexts for this channel
        for (Map.Entry<Long, StreamingContext> entry : contexts.entrySet()) {
            LOG.debug("Cancelling streaming context for txId: {}", entry.getKey());
            entry.getValue().markCancelled();
        }

        // Clear the map
        contexts.clear();
    }

    /**
     * Context for managing streaming state on server side.
     * Implements StreamCancellation but cancel() does nothing as server-side cancellation
     * is initiated by client messages, not by direct server calls.
     */
    private static class StreamingContext implements StreamCancellation {
        private volatile boolean cancelled = false;
        private volatile boolean completed = false;

        @Override
        public void cancel() {
            // NO-OP on server side - cancellation comes from client via protocol messages
            // Server-side user code should not directly cancel streams
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        public void markCancelled() {
            this.cancelled = true;
        }

        public void markCompleted() {
            this.completed = true;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
} 
