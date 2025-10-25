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

package org.bithon.server.datasource.query.pipeline;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Custom Jackson serializer for StringColumn that only serializes elements up to the size boundary.
 * This prevents serializing unused capacity in the underlying array.
 *
 * @author frank.chen021@outlook.com
 * @date 20/10/25
 */
public class StringColumnSerializer extends JsonSerializer<StringColumn> {

    @Override
    public void serialize(StringColumn column, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("name", column.getName());

        gen.writeFieldName("data");
        gen.writeStartArray();
        {
            for (int i = 0, size = column.size(); i < size; i++) {
                gen.writeString(column.getString(i));
            }
        }
        gen.writeEndArray();

        gen.writeEndObject();
    }
}

