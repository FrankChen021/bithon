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

package org.bithon.server.commons.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.bithon.component.commons.utils.HumanReadableNumber;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/3/3 20:12
 */
public class HumanReadableSizeSerializer extends JsonSerializer<HumanReadableNumber> {
    @Override
    public void serialize(HumanReadableNumber value,
                          JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
    }

    @Override
    public Class<HumanReadableNumber> handledType() {
        return HumanReadableNumber.class;
    }
}
