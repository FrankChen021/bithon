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

import org.bithon.component.brpc.message.ServiceMessage;
import org.bithon.component.brpc.message.serializer.Serializer;
import org.bithon.shaded.com.google.protobuf.CodedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author frankchen
 */
public abstract class ServiceMessageOut extends ServiceMessage {

    private Serializer serializer = Serializer.PROTOBUF;

    public Serializer getSerializer() {
        return serializer;
    }

    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    /**
     * NOTE, The implementation MUST call {@link CodedOutputStream#flush()} when exits the function
     */
    public abstract void encode(CodedOutputStream out) throws IOException;

    public byte[] toByteArray() throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024)) {
            CodedOutputStream stream = CodedOutputStream.newInstance(outputStream);
            this.encode(stream);
            return outputStream.toByteArray();
        }
    }
}
