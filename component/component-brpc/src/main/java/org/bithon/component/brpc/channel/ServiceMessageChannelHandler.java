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
import org.bithon.component.brpc.invocation.ClientInvocationManager;
import org.bithon.component.brpc.invocation.IServiceInvocationExecutor;
import org.bithon.component.brpc.invocation.ServiceInvocationRunnable;
import org.bithon.component.brpc.message.ServiceMessage;
import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.brpc.message.in.ServiceRequestMessageIn;
import org.bithon.component.brpc.message.in.ServiceResponseMessageIn;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;
import shaded.io.netty.channel.ChannelHandler;
import shaded.io.netty.channel.ChannelHandlerContext;
import shaded.io.netty.channel.ChannelInboundHandlerAdapter;
import shaded.io.netty.handler.codec.DecoderException;

import java.io.IOException;

@ChannelHandler.Sharable
public class ServiceMessageChannelHandler extends ChannelInboundHandlerAdapter {
    private static final ILogAdaptor log = LoggerFactory.getLogger(ServiceMessageChannelHandler.class);

    private final IServiceInvocationExecutor invoker;
    private final ServiceRegistry serviceRegistry;
    private boolean channelDebugEnabled;

    public ServiceMessageChannelHandler(ServiceRegistry serviceRegistry) {
        this(serviceRegistry, ServiceInvocationRunnable::run);
    }

    public ServiceMessageChannelHandler(ServiceRegistry serviceRegistry, IServiceInvocationExecutor dispatcher) {
        this.serviceRegistry = serviceRegistry;
        this.invoker = dispatcher;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ServiceMessage)) {
            return;
        }

        ServiceMessage message = (ServiceMessage) msg;
        if (message.getMessageType() == ServiceMessageType.CLIENT_REQUEST) {
            ServiceRequestMessageIn request = (ServiceRequestMessageIn) message;
            if (channelDebugEnabled) {
                log.info("receiving request, txId={}, service={}#{}",
                         request.getTransactionId(),
                         request.getServiceName(),
                         request.getMethodName());
            }

            invoker.invoke(new ServiceInvocationRunnable(serviceRegistry,
                                                         ctx.channel(),
                                                         (ServiceRequestMessageIn) message));
        } else if (message.getMessageType() == ServiceMessageType.SERVER_RESPONSE) {
            if (channelDebugEnabled) {
                log.info("receiving response, txId={}", message.getTransactionId());
            }

            ClientInvocationManager.getInstance().onResponse((ServiceResponseMessageIn) message);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof DecoderException) {
            if (cause.getCause() != null) {
                log.warn(cause.getCause().getMessage());
            } else {
                log.warn("Decoder exception: {}", cause.getMessage());
            }
            return;
        }
        if (cause instanceof IOException) {
            // do not log stack trace for known exceptions
            log.error("Exception({}) occurred on channel({} --> {}) when processing message: {}",
                      cause.getClass().getName(),
                      ctx.channel().remoteAddress().toString(),
                      ctx.channel().localAddress().toString(),
                      cause.getMessage());
        } else {
            log.error(StringUtils.format("Exception occurred on channel(%s ---> %s) when processing message",
                                         ctx.channel().remoteAddress().toString(),
                                         ctx.channel().localAddress().toString()),
                      cause);
        }
    }

    public boolean isChannelDebugEnabled() {
        return channelDebugEnabled;
    }

    public void setChannelDebugEnabled(boolean channelDebugEnabled) {
        this.channelDebugEnabled = channelDebugEnabled;
    }
}
