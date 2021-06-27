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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import java.io.IOException;
import java.lang.reflect.Type;

public class JsonSerializer implements ISerializer {
    public static JsonSerializer INSTANCE = new JsonSerializer();

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
}
