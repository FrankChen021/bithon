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

package com.sbss.bithon.component.brpc.message.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * serializer for service arguments and returning object
 */
public enum Serializer {
    BINARY {
        private final ProtocolBufferSerializer serializer = new ProtocolBufferSerializer();

        @Override
        public int getType() {
            return 0x525;
        }

        @Override
        public void serialize(CodedOutputStream os, Object obj) throws IOException {
            if (obj != null) {
                os.writeRawByte(1);
                serializer.serialize(obj, os);
            } else {
                os.writeRawByte(0);
            }
        }

        @Override
        public Object deserialize(CodedInputStream is, Type type) throws IOException {
            if (is.readRawByte() == 1) {
                return serializer.deserialize(is, type);
            } else {
                return null;
            }
        }
    },
    JSON {
        private final ObjectMapper om = new ObjectMapper();

        @Override
        public int getType() {
            return 0x629;
        }

        @Override
        public void serialize(CodedOutputStream os, Object obj) throws IOException {
            if (obj == null) {
                os.writeByteArrayNoTag(new byte[0]);
                return;
            }
            os.writeByteArrayNoTag(om.writeValueAsBytes(obj));
        }

        @Override
        public Object deserialize(CodedInputStream is, Type type) throws IOException {
            byte[] bytes = is.readByteArray();
            if (bytes.length != 0) {
                return om.readValue(bytes, om.constructType(type));
            }
            return null;
        }
    };

    /**
     * type of serializer
     * Encoded in the message for deserialization
     */
    public abstract int getType();

    public abstract void serialize(CodedOutputStream os, Object obj) throws IOException;

    public abstract Object deserialize(CodedInputStream is, Type type) throws IOException;

    public static Serializer getSerializer(int type) {
        if (type == BINARY.getType()) {
            return BINARY;
        }
        if (type == JSON.getType()) {
            return JSON;
        }
        throw new IllegalArgumentException("Unknown serializer: " + type);
    }
}
