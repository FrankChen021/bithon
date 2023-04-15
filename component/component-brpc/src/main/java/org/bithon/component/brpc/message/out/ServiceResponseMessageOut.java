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

import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.brpc.message.serializer.Serializer;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.com.google.protobuf.CodedOutputStream;
import org.bithon.shaded.io.netty.channel.Channel;

import java.io.IOException;

/**
 * @author frankchen
 */
public class ServiceResponseMessageOut extends ServiceMessageOut {
    private long serverResponseAt;
    private Object returning;
    private byte[] returningRaw;
    private Throwable throwable;

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int getMessageType() {
        return ServiceMessageType.SERVER_RESPONSE;
    }

    @Override
    public void encode(CodedOutputStream out) throws IOException {
        out.writeInt32NoTag(this.getMessageType());
        out.writeInt64NoTag(this.getTransactionId());

        out.writeInt64NoTag(serverResponseAt);

        if (this.throwable == null) {
            out.writeRawByte(0);
        } else {
            out.writeRawByte(1);
            out.writeStringNoTag(StringUtils.format("%s %s", this.throwable.getClass().getName(),
                                                    this.throwable.getMessage()));
        }

        if (this.returning == null) {
            if (this.returningRaw == null || this.returningRaw.length == 0) {
                out.writeRawByte(0);
            } else {
                // Placeholder to indicate that there's returning
                out.writeRawByte(1);

                out.writeRawBytes(this.returningRaw);
            }
        } else {
            // Placeholder to indicate that there's returning
            out.writeRawByte(1);

            Serializer serializer = getSerializer();
            out.writeInt32NoTag(serializer.getType());
            serializer.serialize(out, this.returning);
        }
        out.flush();
    }

    public static class Builder {
        ServiceResponseMessageOut response = new ServiceResponseMessageOut();

        public Builder serverResponseAt(long currentTimeMillis) {
            response.serverResponseAt = currentTimeMillis;
            return this;
        }

        public Builder txId(long txId) {
            response.transactionId = txId;
            return this;
        }

        public Builder exception(Throwable throwable) {
            response.throwable = throwable;
            return this;
        }

        public Builder returning(Object ret) {
            response.returning = ret;
            response.returningRaw = null;
            return this;
        }

        public Builder returningRaw(byte[] returningRaw) {
            response.returningRaw = returningRaw;
            response.returning = null;
            return this;
        }

        public Builder serializer(Serializer serializer) {
            response.setSerializer(serializer);
            return this;
        }

        public ServiceResponseMessageOut build() {
            return response;
        }

        public void send(Channel channel) {
            channel.writeAndFlush(response);
        }
    }
}
