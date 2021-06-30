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

package com.sbss.bithon.component.brpc.message.in;

import com.google.protobuf.CodedInputStream;
import com.sbss.bithon.component.brpc.message.ServiceMessage;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class ServiceMessageIn extends ServiceMessage {

    public abstract ServiceMessage decode(CodedInputStream in) throws IOException;

    protected CharSequence readString(ByteBuf in) {
        int len = in.readInt();
        if (len == 0) {
            return null;
        }
        return in.readCharSequence(len, StandardCharsets.UTF_8);
    }

    protected byte[] readBytes(ByteBuf in) {
        int len = in.readInt();
        if (len > 0) {
            byte[] bytes = new byte[len];
            in.readBytes(bytes);
            return bytes;
        } else {
            return null;
        }
    }
}
