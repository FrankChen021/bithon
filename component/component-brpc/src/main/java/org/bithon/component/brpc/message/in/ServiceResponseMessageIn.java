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
import org.bithon.component.brpc.message.serializer.Serializer;
import org.bithon.shaded.com.google.protobuf.CodedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * @author frankchen
 */
public class ServiceResponseMessageIn extends ServiceMessageIn {
    private long serverResponseAt;
    private CodedInputStream returning;
    private String exception;

    @Override
    public int getMessageType() {
        return ServiceMessageType.SERVER_RESPONSE;
    }

    @Override
    public ServiceMessage decode(CodedInputStream in) throws IOException {
        this.transactionId = in.readInt64();

        this.serverResponseAt = in.readInt64();

        boolean hasException = in.readRawByte() == 1;
        if (hasException) {
            this.exception = in.readString();
        }

        boolean hasReturning = in.readRawByte() == 1;
        if (hasReturning) {
            this.returning = in;
        }
        return this;
    }

    public long getServerResponseAt() {
        return serverResponseAt;
    }

    public Object getReturningAsObject(Type type) throws IOException {
        if (returning != null) {
            int serializer = this.returning.readInt32();
            return Serializer.getSerializer(serializer).deserialize(this.returning, type);
        }
        return null;
    }

    public byte[] getReturnAsRaw() throws IOException {
        if (returning != null) {
            return returning.readRawBytes(returning.getBytesUntilLimit());
        }
        return new byte[0];
    }

    public String getException() {
        return exception;
    }

    public static ServiceResponseMessageIn from(InputStream inputStream) throws IOException {
        CodedInputStream is = CodedInputStream.newInstance(inputStream);
        is.pushLimit(inputStream.available());
        int messageType = is.readInt32();
        if (messageType == ServiceMessageType.SERVER_RESPONSE) {
            return (ServiceResponseMessageIn) new ServiceResponseMessageIn().decode(is);
        }
        throw new BadRequestException("messageType [%x] is not a valid ServiceResponseMessageIn message", messageType);
    }
}
