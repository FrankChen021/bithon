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

package org.bithon.component.brpc.channel;

import org.bithon.component.brpc.ServiceRegistry;
import org.bithon.component.brpc.exception.BadRequestException;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.ServiceNotFoundException;
import org.bithon.component.brpc.invocation.InvocationManager;
import org.bithon.component.brpc.invocation.ServiceInvocationRunnable;
import org.bithon.component.brpc.invocation.ServiceStreamingInvocationRunnable;
import org.bithon.component.brpc.message.ServiceMessage;
import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.brpc.message.in.ServiceMessageIn;
import org.bithon.component.brpc.message.in.ServiceRequestMessageIn;
import org.bithon.component.brpc.message.in.ServiceResponseMessageIn;
import org.bithon.component.brpc.message.in.ServiceStreamingDataMessageIn;
import org.bithon.component.brpc.message.in.ServiceStreamingEndMessageIn;
import org.bithon.component.brpc.message.out.ServiceStreamingEndMessageOut;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.component.commons.utils.TimeWindowBasedCounter;
import org.bithon.shaded.io.netty.channel.Channel;
import org.bithon.shaded.io.netty.channel.ChannelHandlerContext;
import org.bithon.shaded.io.netty.channel.SimpleChannelInboundHandler;
import org.bithon.shaded.io.netty.handler.codec.DecoderException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * @author frankchen
 */
class ServiceMessageChannelHandler extends SimpleChannelInboundHandler<ServiceMessage> {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ServiceMessageChannelHandler.class);

    private final Executor executor;
    private final ServiceRegistry serviceRegistry;
    private final InvocationManager invocationManager;

    /**
     * client or server id for logging purpose
     */
    private final String id;
    private final TimeWindowBasedCounter unwritableCounter = new TimeWindowBasedCounter(Duration.ofMinutes(1));

    /**
     * Instantiate an instance which calls the service in specified executor.
     */
    public ServiceMessageChannelHandler(String id,
                                        ServiceRegistry serviceRegistry,
                                        Executor executor,
                                        InvocationManager invocationManager) {
        this.id = id;
        this.serviceRegistry = Preconditions.checkArgumentNotNull("serviceRegistry", serviceRegistry);
        this.invocationManager = Preconditions.checkArgumentNotNull("invocationManager", invocationManager);
        this.executor = executor == null ? Runnable::run : executor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServiceMessage msg) {
        try {
            handleMessageIn(ctx.channel(), (ServiceMessageIn) msg);
        } finally {
            try {
                // Make sure all content has been consumed
                ((ServiceMessageIn) msg).consume();
            } catch (Throwable ignored) {
            }
        }
    }

    private void handleMessageIn(Channel channel, ServiceMessageIn msg) {
        switch (msg.getMessageType()) {
            case ServiceMessageType.CLIENT_REQUEST_ONEWAY:
            case ServiceMessageType.CLIENT_REQUEST:
            case ServiceMessageType.CLIENT_REQUEST_V2:
                ServiceRequestMessageIn request = (ServiceRequestMessageIn) msg;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Receiving request, txId={}, service={}#{}",
                              request.getTransactionId(),
                              request.getServiceName(),
                              request.getMethodName());
                }

                ServiceInvocationRunnable.execute(serviceRegistry,
                                                  channel,
                                                  (ServiceRequestMessageIn) msg,
                                                  this.executor);
                break;

            case ServiceMessageType.CLIENT_STREAMING_REQUEST:
                ServiceRequestMessageIn streamingRequest = (ServiceRequestMessageIn) msg;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Receiving streaming request, txId={}, service={}#{}",
                              streamingRequest.getTransactionId(),
                              streamingRequest.getServiceName(),
                              streamingRequest.getMethodName());
                }

                handleStreamingRequest(serviceRegistry,
                                       channel,
                                       streamingRequest,
                                       this.executor);
                break;

            case ServiceMessageType.SERVER_RESPONSE:
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Receiving response, txId={}", msg.getTransactionId());
                }

                invocationManager.handleResponse((ServiceResponseMessageIn) msg);
                break;

            case ServiceMessageType.SERVER_STREAMING_DATA:
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Receiving streaming data, txId={}",
                              msg.getTransactionId());
                }

                invocationManager.handleStreamingData((ServiceStreamingDataMessageIn) msg);
                break;

            case ServiceMessageType.SERVER_STREAMING_END:
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Receiving streaming end, txId={}",
                              msg.getTransactionId());
                }

                invocationManager.handleStreamingEnd((ServiceStreamingEndMessageIn) msg);
                break;

            case ServiceMessageType.CLIENT_STREAMING_CANCEL:
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Receiving streaming cancel, txId={}",
                              msg.getTransactionId());
                }

                ServiceStreamingInvocationRunnable.cancelStreaming(channel, msg.getTransactionId());
                break;

            default:
                LOG.warn("[{}] - Receiving unknown message: {}", this.id, msg.getMessageType());
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof DecoderException) {
            if (cause.getCause() != null) {
                LOG.warn(cause.getCause().getMessage());
            } else {
                LOG.warn("Decoder exception: {}", cause.getMessage());
            }
            return;
        }

        if (cause instanceof IOException) {
            // do not log stack trace for known exceptions
            LOG.error("[{}] - Exception({}) occurred on channel({} --> {}) when processing message: {}",
                      this.id,
                      cause.getClass().getName(),
                      ctx.channel().remoteAddress().toString(),
                      ctx.channel().localAddress().toString(),
                      cause.getMessage());
        } else {
            LOG.error(StringUtils.format("[%s] - Exception occurred on channel(%s ---> %s) when processing message",
                                         this.id,
                                         ctx.channel().remoteAddress().toString(),
                                         ctx.channel().localAddress().toString()), cause);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        if (ctx.channel().isWritable()) {
            // Set auto read to true if the channel is writable.
            ctx.channel().config().setAutoRead(true);
        } else {
            ctx.channel().config().setAutoRead(false);

            long accumulatedCount = this.unwritableCounter.add(1);
            if (accumulatedCount > 0) {
                LOG.warn("[{}] - Channel is not writable for {} times in the past 1 min", this.id, accumulatedCount);
            }
        }

        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            // Clean up streaming contexts on server side
            ServiceStreamingInvocationRunnable.cleanupForChannel(ctx.channel());

            // Clean up streaming requests on client side
            invocationManager.handleChannelClosure();
        } catch (Exception e) {
            LOG.error("[{}] Error during channel cleanup", id, e);
        } finally {
            super.channelInactive(ctx);
        }
    }

    private void handleStreamingRequest(ServiceRegistry serviceRegistry,
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
                LOG.error(StringUtils.format("[Client=%s] Streaming Service Invocation on %s#%s",
                                             channel.remoteAddress().toString(),
                                             serviceRequest.getServiceName(),
                                             serviceRequest.getMethodName()),
                          cause);
            }

            try {
                new ServiceStreamingEndMessageOut(serviceRequest.getTransactionId(), cause).send(channel);
            } catch (Exception sendException) {
                LOG.error("Failed to send streaming error response", sendException);
            }
        }
    }
}
