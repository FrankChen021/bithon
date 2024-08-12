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
import org.bithon.component.brpc.invocation.InvocationManager;
import org.bithon.component.brpc.invocation.ServiceInvocationRunnable;
import org.bithon.component.brpc.message.ServiceMessage;
import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.brpc.message.in.ServiceRequestMessageIn;
import org.bithon.component.brpc.message.in.ServiceResponseMessageIn;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.io.netty.channel.ChannelHandler;
import org.bithon.shaded.io.netty.channel.ChannelHandlerContext;
import org.bithon.shaded.io.netty.channel.SimpleChannelInboundHandler;
import org.bithon.shaded.io.netty.handler.codec.DecoderException;

import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * @author frankchen
 */
@ChannelHandler.Sharable
class ServiceMessageChannelHandler extends SimpleChannelInboundHandler<ServiceMessage> {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ServiceMessageChannelHandler.class);

    private final Executor executor;
    private final ServiceRegistry serviceRegistry;
    private final InvocationManager invocationManager;

    /**
     * Instantiate an instance which calls the service in specified executor.
     */
    public ServiceMessageChannelHandler(ServiceRegistry serviceRegistry,
                                        Executor executor,
                                        InvocationManager invocationManager) {
        this.serviceRegistry = Preconditions.checkArgumentNotNull("serviceRegistry", serviceRegistry);
        this.invocationManager = Preconditions.checkArgumentNotNull("invocationManager", invocationManager);
        this.executor = executor == null ? Runnable::run : executor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServiceMessage msg) {
        switch (msg.getMessageType()) {
            case ServiceMessageType.CLIENT_REQUEST_ONEWAY:
            case ServiceMessageType.CLIENT_REQUEST:
            case ServiceMessageType.CLIENT_REQUEST_V2:
                ServiceRequestMessageIn request = (ServiceRequestMessageIn) msg;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Receiving request, txId={}, service={}#{}", request.getTransactionId(), request.getServiceName(), request.getMethodName());
                }

                executor.execute(new ServiceInvocationRunnable(serviceRegistry, ctx.channel(), (ServiceRequestMessageIn) msg));
                break;

            case ServiceMessageType.SERVER_RESPONSE:
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Receiving response, txId={}", msg.getTransactionId());
                }

                invocationManager.handleResponse((ServiceResponseMessageIn) msg);
                break;

            default:
                LOG.warn("Receiving unknown message: {}", msg.getMessageType());
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
            LOG.error("Exception({}) occurred on channel({} --> {}) when processing message: {}",
                      cause.getClass().getName(),
                      ctx.channel().remoteAddress().toString(),
                      ctx.channel().localAddress().toString(),
                      cause.getMessage());
        } else {
            LOG.error(StringUtils.format("Exception occurred on channel(%s ---> %s) when processing message",
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
            LOG.warn("channel is not writable, disable auto reading for back pressing");
            ctx.channel().config().setAutoRead(false);
            ctx.flush();
        }
        ctx.fireChannelWritabilityChanged();
    }
}
