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

package cn.bithon.rpc.message.serializer;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * serializer for service arguments and returning object
 */
public interface ISerializer {
    /**
     * type of serializer
     * Encoded in the message for deserialization
     */
    int getType();

    void serialize(CodedOutputStream os, Object obj) throws IOException;

    Object deserialize(CodedInputStream is, Type type) throws IOException;
}
