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

package org.bithon.component.brpc.message.serializer;

import shaded.com.fasterxml.jackson.core.JsonFactory;
import shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import shaded.com.fasterxml.jackson.databind.ObjectMapper;
import shaded.com.fasterxml.jackson.databind.SerializationFeature;
import shaded.com.fasterxml.jackson.dataformat.smile.SmileFactory;
import shaded.com.fasterxml.jackson.dataformat.smile.SmileGenerator;

public class DefaultObjectMapper extends ObjectMapper {

    private DefaultObjectMapper(DefaultObjectMapper mapper) {
        super(mapper);
    }

    private DefaultObjectMapper(JsonFactory factory) {
        super(factory);

        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // See https://github.com/FasterXML/jackson-databind/issues/170
        // configure(MapperFeature.AUTO_DETECT_CREATORS, false);
        configure(SerializationFeature.INDENT_OUTPUT, false);
        configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, false);
    }

    @Override
    public ObjectMapper copy() {
        return new DefaultObjectMapper(this);
    }

    public static ObjectMapper createInstance() {
        final SmileFactory smileFactory = new SmileFactory();
        smileFactory.configure(SmileGenerator.Feature.ENCODE_BINARY_AS_7BIT, false);
        smileFactory.delegateToTextual(true);
        DefaultObjectMapper mapper = new DefaultObjectMapper(smileFactory);
        mapper.getFactory().setCodec(mapper);
        return mapper;
    }
}

