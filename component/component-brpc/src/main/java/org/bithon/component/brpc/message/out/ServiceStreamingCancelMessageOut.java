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
import org.bithon.shaded.com.google.protobuf.CodedOutputStream;
import org.bithon.shaded.io.netty.channel.Channel;

import java.io.IOException;

/**
 * Message for cancelling a streaming RPC call from client side
 * 
 * @author frankchen
 */
public class ServiceStreamingCancelMessageOut extends ServiceMessageOut {
    
    private final long transactionId;
    
    public ServiceStreamingCancelMessageOut(long transactionId) {
        this.transactionId = transactionId;
    }
    
    @Override
    public int getMessageType() {
        return ServiceMessageType.CLIENT_STREAMING_CANCEL;
    }
    
    @Override
    public long getTransactionId() {
        return transactionId;
    }
    
    @Override
    public void encode(CodedOutputStream out) throws IOException {
        out.writeInt32NoTag(this.getMessageType());
        out.writeInt64NoTag(this.transactionId);
        out.flush();
    }
    
    /**
     * Send this streaming cancel message to the channel
     */
    public void send(Channel channel) {
        try {
            channel.writeAndFlush(this.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to send streaming cancel message", e);
        }
    }
}
