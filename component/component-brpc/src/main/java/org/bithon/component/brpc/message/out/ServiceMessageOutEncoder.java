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

package org.bithon.component.brpc.message.out;

import org.bithon.component.brpc.invocation.InvocationManager;
import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.logging.RateLimitedLogger;
import org.bithon.shaded.com.google.protobuf.CodedOutputStream;
import org.bithon.shaded.io.netty.buffer.ByteBuf;
import org.bithon.shaded.io.netty.buffer.ByteBufOutputStream;
import org.bithon.shaded.io.netty.channel.ChannelFutureListener;
import org.bithon.shaded.io.netty.channel.ChannelHandlerContext;
import org.bithon.shaded.io.netty.channel.ChannelPromise;
import org.bithon.shaded.io.netty.handler.codec.EncoderException;
import org.bithon.shaded.io.netty.handler.codec.MessageToByteEncoder;

import java.nio.channels.ClosedChannelException;
import java.time.Duration;

/**
 * @author frankchen
 */
public class ServiceMessageOutEncoder extends MessageToByteEncoder<ServiceMessageOut> {
    // Rate-limited logger to prevent log flooding - logs at most once per minute for each exception type
    private final RateLimitedLogger LOG = new RateLimitedLogger(LoggerFactory.getLogger(ServiceMessageOutEncoder.class),
                                                                Duration.ofMinutes(1));

    private final InvocationManager invocationManager;

    public ServiceMessageOutEncoder(InvocationManager invocationManager) {
        this.invocationManager = invocationManager;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ServiceMessageOut msg, ByteBuf out) {
        try {
            CodedOutputStream os = CodedOutputStream.newInstance(new ByteBufOutputStream(out));
            msg.encode(os);
        } catch (Exception e) {
            throw new ServiceMessageEncodingException(e);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ServiceMessageOut out = (ServiceMessageOut) msg;
        final int messageType = out.getMessageType();
        final long txId = out.getTransactionId();

        super.write(ctx,
                    msg,
                    promise.addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            return;
                        }

                        // Handle encoding exception
                        Throwable cause = future.cause();
                        if (messageType == ServiceMessageType.CLIENT_REQUEST
                            || messageType == ServiceMessageType.CLIENT_REQUEST_V2
                            || messageType == ServiceMessageType.CLIENT_STREAMING_REQUEST
                        ) {
                            invocationManager.handleException(txId, cause.getCause());

                            // The exception is propagated to caller, so we do not need to log it here
                            return;
                        }

                        // Logging for problem addressing with rate limiting to prevent log flooding
                        if (cause instanceof ClosedChannelException) {
                            LOG.warn(cause, "Failed to send message due to channel closed: {}", cause.getMessage());
                        } else {
                            LOG.warn(cause, "Failed to send message: {}", cause);
                        }
                    }));
    }

    static class ServiceMessageEncodingException extends EncoderException {
        public ServiceMessageEncodingException(Throwable cause) {
            super(cause);
        }
    }
}
