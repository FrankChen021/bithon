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
import org.bithon.shaded.com.google.protobuf.CodedOutputStream;
import org.bithon.shaded.io.netty.channel.Channel;

import java.io.IOException;

/**
 * Message for streaming data from server to client
 * 
 * @author frankchen
 */
public class ServiceStreamingDataMessageOut extends ServiceMessageOut {
    
    private final Object data;
    
    public ServiceStreamingDataMessageOut(long transactionId, Object data, Serializer serializer) {
        this.transactionId = transactionId;
        this.data = data;
        this.setSerializer(serializer);
    }
    
    @Override
    public int getMessageType() {
        return ServiceMessageType.SERVER_STREAMING_DATA;
    }
    
    @Override
    public void encode(CodedOutputStream out) throws IOException {
        out.writeInt32NoTag(this.getMessageType());
        out.writeInt64NoTag(this.transactionId);
        
        // Serialize the data
        Serializer serializer = getSerializer();
        out.writeInt32NoTag(serializer.getType());
        serializer.serialize(out, this.data);
        
        out.flush();
    }
    
    /**
     * Send this streaming data message to the channel
     */
    public void send(Channel channel) {
        channel.writeAndFlush(this);
    }
}
