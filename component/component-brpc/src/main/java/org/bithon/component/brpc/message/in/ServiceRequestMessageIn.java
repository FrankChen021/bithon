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
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.brpc.message.ServiceMessage;
import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.brpc.message.serializer.Serializer;
import org.bithon.shaded.com.google.protobuf.CodedInputStream;
import org.bithon.shaded.com.google.protobuf.CodedOutputStream;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Locale;

/**
 * @author frankchen
 */
public class ServiceRequestMessageIn extends ServiceMessageIn {

    private final int messageType;
    private String serviceName;
    private String methodName;
    private String appName;
    private Headers headers = Headers.EMPTY;

    /**
     * args
     */
    private CodedInputStream argsInputStream;
    private Serializer serializer;

    public ServiceRequestMessageIn(int messageType) {
        this.messageType = messageType;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public int getMessageType() {
        return messageType;
    }

    @Override
    public ServiceMessage decode(CodedInputStream in) throws IOException {
        this.transactionId = in.readInt64();
        this.serviceName = in.readString();
        this.methodName = in.readString();

        boolean hasAppName = in.readRawByte() == 1;
        if (hasAppName) {
            appName = in.readString();
        }

        this.serializer = Serializer.getSerializer(in.readInt32());

        // Header
        if (messageType == ServiceMessageType.CLIENT_REQUEST_V2) {
            this.headers = (Headers) this.serializer.deserialize(in, Headers.class);
        }

        this.argsInputStream = in;
        return this;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getAppName() {
        return appName;
    }

    public Headers getHeaders() {
        return headers;
    }

    public Object[] readArgs(Type[] parameterTypes) throws BadRequestException, IOException {
        int argLength = this.argsInputStream.readInt32();
        if (argLength != parameterTypes.length) {
            throw new BadRequestException(String.format(Locale.ENGLISH,
                                                        "Argument size not match. Expected %d, but given %d",
                                                        parameterTypes.length,
                                                        argLength));
        }

        Object[] inputArgs = new Object[argLength];
        for (int i = 0; i < argLength; i++) {
            try {
                inputArgs[i] = serializer.deserialize(this.argsInputStream, parameterTypes[i]);
            } catch (IOException | IllegalStateException e) {
                throw new BadRequestException("Bad args for %s#%s: %s",
                                              serviceName,
                                              methodName,
                                              e.getMessage());
            }
        }
        return inputArgs;
    }

    public byte[] getRawArgs() {
        CodedOutputStream os;

        // TODO: HOW TO READ ALL REMAINING DATA???
        //this.argsInputStream.readRawBytes(this.argsInputStream.readByteBuffer())
        return null;
    }

    public static ServiceRequestMessageIn from(CodedInputStream inputStream) throws IOException {
        int messageType = inputStream.readInt32();
        if (messageType == ServiceMessageType.CLIENT_REQUEST
                || messageType == ServiceMessageType.CLIENT_REQUEST_ONEWAY
                || messageType == ServiceMessageType.CLIENT_REQUEST_V2) {
            return (ServiceRequestMessageIn) new ServiceRequestMessageIn(messageType).decode(inputStream);
        } else {
            throw new BadRequestException("messageType [%x] is not a valid ServiceRequest message", messageType);
        }
    }
}
