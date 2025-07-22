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
import org.bithon.shaded.com.google.protobuf.CodedInputStream;
import org.bithon.shaded.io.netty.buffer.ByteBuf;
import org.bithon.shaded.io.netty.buffer.ByteBufInputStream;
import org.bithon.shaded.io.netty.channel.ChannelHandlerContext;
import org.bithon.shaded.io.netty.handler.codec.ByteToMessageDecoder;

import java.io.IOException;
import java.util.List;

/**
 * Decode input stream to an incoming service message, either it's {@link ServiceRequestMessageIn}
 * or {@link ServiceResponseMessageIn}
 * <p>
 *
 * NOTE that the {@link ByteToMessageDecoder} DOES NOT allow its subclasses to be {@link org.bithon.shaded.io.netty.channel.ChannelHandler.Sharable}.
 * However, {@link org.bithon.shaded.io.netty.handler.codec.MessageToByteEncoder} CAN BE shared
 *
 * @author frankchen
 */
public class ServiceMessageInDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws IOException {
        if (in.readableBytes() < 4) {
            // This might be a fragment packet
            return;
        }

        CodedInputStream is = CodedInputStream.newInstance(new ByteBufInputStream(in));

        // The CodedInputStream does not provide an API to get the length of unread data,
        // So, we need to set the limit first and then use getBytesUntilLimit() as a workaround
        is.pushLimit(in.readableBytes());

        int messageType = is.readInt32();
        switch (messageType) {
            case ServiceMessageType.CLIENT_REQUEST:
            case ServiceMessageType.CLIENT_REQUEST_ONEWAY:
            case ServiceMessageType.CLIENT_REQUEST_V2:
            case ServiceMessageType.CLIENT_STREAMING_REQUEST:
                out.add(new ServiceRequestMessageIn(messageType, is).decode());
                break;

            //case ServiceMessageType.CLIENT_STREAMING_REQUEST:
            //    out.add(new ServiceStreamingRequestMessageIn(messageType, is).decode());
            //    break;

            case ServiceMessageType.SERVER_RESPONSE:
                out.add(new ServiceResponseMessageIn(is).decode());
                break;

            case ServiceMessageType.SERVER_STREAMING_DATA:
                out.add(new ServiceStreamingDataMessageIn(is).decode());
                break;

            case ServiceMessageType.SERVER_STREAMING_END:
                out.add(new ServiceStreamingEndMessageIn(is).decode());
                break;

            case ServiceMessageType.CLIENT_STREAMING_CANCEL:
                out.add(new ServiceStreamingCancelMessageIn(is).decode());
                break;

            default:
                throw new UnknownMessageException(ctx.channel().remoteAddress().toString(),
                                                  ctx.channel().localAddress().toString(),
                                                  messageType);
        }
    }
}
