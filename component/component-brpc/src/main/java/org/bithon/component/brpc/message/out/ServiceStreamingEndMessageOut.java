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
 * Message for indicating the end of a streaming RPC call
 * 
 * @author frankchen
 */
public class ServiceStreamingEndMessageOut extends ServiceMessageOut {
    
    private final Throwable exception;
    
    /**
     * Constructor for successful completion
     */
    public ServiceStreamingEndMessageOut(long transactionId) {
        this.transactionId = transactionId; // Set the base class field
        this.exception = null;
    }
    
    /**
     * Constructor for error completion
     */
    public ServiceStreamingEndMessageOut(long transactionId, Throwable exception) {
        this.transactionId = transactionId; // Set the base class field
        this.exception = exception;
    }
    
    @Override
    public int getMessageType() {
        return ServiceMessageType.SERVER_STREAMING_END;
    }
    
    public boolean hasException() {
        return exception != null;
    }
    
    public Throwable getException() {
        return exception;
    }
    
    @Override
    public void encode(CodedOutputStream out) throws IOException {
        out.writeInt32NoTag(this.getMessageType());
        out.writeInt64NoTag(this.transactionId);
        
        // Write exception flag and data
        if (exception == null) {
            out.writeRawByte(0);
        } else {
            out.writeRawByte(1);
            out.writeStringNoTag(exception.getClass().getName() + ": " + exception.getMessage());
        }
        
        out.flush();
    }
    
    /**
     * Send this streaming end message to the channel
     */
    public void send(Channel channel) {
        channel.writeAndFlush(this);
    }
} 
