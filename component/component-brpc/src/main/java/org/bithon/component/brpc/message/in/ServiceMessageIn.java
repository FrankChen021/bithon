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

import org.bithon.component.brpc.exception.BadRequestException;
import org.bithon.component.brpc.message.ServiceMessage;
import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.shaded.com.google.protobuf.CodedInputStream;

import java.io.IOException;

/**
 * @author frankchen
 */
public abstract class ServiceMessageIn extends ServiceMessage {

    protected final CodedInputStream in;

    protected ServiceMessageIn(CodedInputStream in) {
        this.in = in;
    }

    public abstract ServiceMessage decode() throws IOException;

    /**
     * Consume the message.
     */
    public void consume() throws IOException {
        int unConsumedBytes = in.getBytesUntilLimit();
        if (unConsumedBytes > 0) {
            in.skipRawBytes(unConsumedBytes);
        }
    }

    public static ServiceMessageIn from(byte[] bytes) throws IOException {
        CodedInputStream inputStream = CodedInputStream.newInstance(bytes);
        int messageType = inputStream.readInt32();
        if (messageType == ServiceMessageType.CLIENT_REQUEST
            || messageType == ServiceMessageType.CLIENT_REQUEST_ONEWAY
            || messageType == ServiceMessageType.CLIENT_REQUEST_V2
            || messageType == ServiceMessageType.CLIENT_STREAMING_REQUEST
        ) {
            return (ServiceMessageIn) new ServiceRequestMessageIn(messageType, inputStream).decode();
        }

        if (messageType == ServiceMessageType.SERVER_STREAMING_DATA) {
            return (ServiceMessageIn) new ServiceStreamingDataMessageIn(inputStream).decode();
        }

        if (messageType == ServiceMessageType.SERVER_STREAMING_END) {
            return (ServiceMessageIn) new ServiceStreamingEndMessageIn(inputStream).decode();
        }

        throw new BadRequestException("messageType [%x] is not a valid ServiceRequest message", messageType);

    }
}
