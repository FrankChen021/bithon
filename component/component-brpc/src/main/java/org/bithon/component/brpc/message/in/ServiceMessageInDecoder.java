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

package org.bithon.component.brpc.message.in;

import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.brpc.message.UnknownMessageException;
import shaded.com.google.protobuf.CodedInputStream;
import shaded.io.netty.buffer.ByteBuf;
import shaded.io.netty.buffer.ByteBufInputStream;
import shaded.io.netty.channel.ChannelHandlerContext;
import shaded.io.netty.handler.codec.ByteToMessageDecoder;

import java.io.IOException;
import java.util.List;

/**
 * Decode input stream to incoming service message, either it's {@link ServiceRequestMessageIn}
 * or {@link ServiceResponseMessageIn}
 *
 * @author frankchen
 */
public class ServiceMessageInDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws IOException {
        if (in.readableBytes() < 4) {
            // this might be a fragment packet
            return;
        }

        CodedInputStream is = CodedInputStream.newInstance(new ByteBufInputStream(in));
        int messageType = is.readInt32();
        if (messageType == ServiceMessageType.CLIENT_REQUEST
            || messageType == ServiceMessageType.CLIENT_REQUEST_ONEWAY
            || messageType == ServiceMessageType.CLIENT_REQUEST_V2) {
            out.add(new ServiceRequestMessageIn(messageType).decode(is));
        } else if (messageType == ServiceMessageType.SERVER_RESPONSE) {
            out.add(new ServiceResponseMessageIn().decode(is));
        } else {
            throw new UnknownMessageException(ctx.channel().remoteAddress().toString(),
                                              ctx.channel().localAddress().toString(),
                                              messageType);
        }
    }
}
