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

import org.bithon.component.brpc.message.ServiceMessage;
import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.brpc.message.serializer.Serializer;
import org.bithon.shaded.com.google.protobuf.CodedInputStream;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Message for receiving streaming data on the client side
 * 
 * @author frankchen
 */
public class ServiceStreamingDataMessageIn extends ServiceMessageIn {
    
    private CodedInputStream dataStream;
    private int serializerType;
    
    @Override
    public int getMessageType() {
        return ServiceMessageType.SERVER_STREAMING_DATA;
    }
    
    @Override
    public ServiceMessage decode(CodedInputStream in) throws IOException {
        this.transactionId = in.readInt64();
        
        // Read and store serializer type
        this.serializerType = in.readInt32();
        
        // Store the remaining stream for data deserialization
        this.dataStream = in;
        
        return this;
    }
    
    /**
     * Deserialize the streaming data to the specified type
     */
    public Object getData(Type type) throws IOException {
        if (dataStream != null) {
            return Serializer.getSerializer(serializerType).deserialize(dataStream, type);
        }
        return null;
    }
    
    /**
     * Get the raw data bytes
     */
    public byte[] getRawData() throws IOException {
        if (dataStream != null) {
            return dataStream.readRawBytes(dataStream.getBytesUntilLimit());
        }
        return new byte[0];
    }
}
