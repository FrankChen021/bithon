/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.component.brpc.channel;

import com.sbss.bithon.component.brpc.ServiceRegistry;
import com.sbss.bithon.component.brpc.invocation.ClientInvocationManager;
import com.sbss.bithon.component.brpc.invocation.IServiceInvocationExecutor;
import com.sbss.bithon.component.brpc.invocation.ServiceInvocationRunnable;
import com.sbss.bithon.component.brpc.message.ServiceMessage;
import com.sbss.bithon.component.brpc.message.ServiceMessageType;
import com.sbss.bithon.component.brpc.message.in.ServiceRequestMessageIn;
import com.sbss.bithon.component.brpc.message.in.ServiceResponseMessageIn;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class ServiceMessageChannelHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ServiceMessageChannelHandler.class);

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
        log.error("Exception occurred when processing message", cause);
    }

    public boolean isChannelDebugEnabled() {
        return channelDebugEnabled;
    }

    public void setChannelDebugEnabled(boolean channelDebugEnabled) {
        this.channelDebugEnabled = channelDebugEnabled;
    }
}
