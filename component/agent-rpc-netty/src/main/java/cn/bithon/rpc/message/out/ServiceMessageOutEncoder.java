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

package cn.bithon.rpc.message.out;

import com.google.protobuf.CodedOutputStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ServiceMessageOutEncoder extends MessageToByteEncoder<ServiceMessageOut> {
    private static final Logger log = LoggerFactory.getLogger(ServiceMessageOutEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, ServiceMessageOut msg, ByteBuf out) {
        try {
            CodedOutputStream os = CodedOutputStream.newInstance(new ByteBufOutputStream(out));
            msg.encode(os);
            os.flush();
        } catch (IOException e) {
            log.error("Exception when encoding out message", e);
        }
    }
}
