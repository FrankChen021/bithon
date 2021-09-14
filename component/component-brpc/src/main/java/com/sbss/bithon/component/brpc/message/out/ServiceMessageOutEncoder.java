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

package com.sbss.bithon.component.brpc.message.out;

import com.sbss.bithon.component.brpc.invocation.ClientInvocationManager;
import com.sbss.bithon.component.brpc.message.ServiceMessageType;
import shaded.com.google.protobuf.CodedOutputStream;
import shaded.io.netty.buffer.ByteBuf;
import shaded.io.netty.buffer.ByteBufOutputStream;
import shaded.io.netty.channel.ChannelFutureListener;
import shaded.io.netty.channel.ChannelHandlerContext;
import shaded.io.netty.channel.ChannelPromise;
import shaded.io.netty.handler.codec.EncoderException;
import shaded.io.netty.handler.codec.MessageToByteEncoder;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

public class ServiceMessageOutEncoder extends MessageToByteEncoder<ServiceMessageOut> {
    private static final Logger log = LoggerFactory.getLogger(ServiceMessageOutEncoder.class);

    static class ServiceMessageEncodingException extends EncoderException {
        final ServiceMessageOut out;

        public ServiceMessageEncodingException(ServiceMessageOut out, Throwable cause) {
            super(cause);
            this.out = out;
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ServiceMessageOut msg, ByteBuf out) {
        try {
            CodedOutputStream os = CodedOutputStream.newInstance(new ByteBufOutputStream(out));
            msg.encode(os);
            os.flush();
        } catch (Exception e) {
            throw new ServiceMessageEncodingException(msg, e);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                return;
            }

            // Handle encoding exception
            Throwable cause = future.cause();
            if (cause instanceof ServiceMessageEncodingException) {
                ServiceMessageOut out = ((ServiceMessageEncodingException) cause).out;
                if (out.getMessageType() == ServiceMessageType.CLIENT_REQUEST) {
                    ClientInvocationManager.getInstance()
                                           .onClientException(((ServiceMessageEncodingException) cause).out.getTransactionId(),
                                                              cause.getCause());
                    return;
                }
            }

            log.error("Exception when encoding out message", cause);
        }));
    }
}
