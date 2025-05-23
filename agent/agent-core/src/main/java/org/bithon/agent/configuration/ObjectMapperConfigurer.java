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

package org.bithon.agent.configuration;

import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.shaded.com.fasterxml.jackson.core.JsonGenerator;
import org.bithon.shaded.com.fasterxml.jackson.core.JsonParser;
import org.bithon.shaded.com.fasterxml.jackson.databind.DeserializationContext;
import org.bithon.shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonDeserializer;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonSerializer;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.shaded.com.fasterxml.jackson.databind.SerializerProvider;
import org.bithon.shaded.com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

/**
 * @author Frank Chen
 * @date 5/2/24 2:40 pm
 */
public class ObjectMapperConfigurer {

    private static final SimpleModule MODULE = new SimpleModule();

    static {
        MODULE.addDeserializer(HumanReadablePercentage.class, new HumanReadablePercentageDeserializer())
              .addSerializer(HumanReadablePercentage.class, new HumanReadablePercentageSerializer())
              .addDeserializer(HumanReadableDuration.class, new HumanReadableDurationDeserializer())
              .addSerializer(HumanReadableDuration.class, new HumanReadableDurationSerializer())
              .addDeserializer(HumanReadableNumber.class, new HumanReadableNumberDeserializer())
              .addSerializer(HumanReadableNumber.class, new HumanReadableNumberSerializer());
    }

    public static ObjectMapper configure(ObjectMapper objectMapper) {
        return objectMapper.registerModule(MODULE)
                           .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                           .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    }

    static class HumanReadablePercentageSerializer extends JsonSerializer<HumanReadablePercentage> {
        @Override
        public void serialize(HumanReadablePercentage value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.toString());
        }

        @Override
        public Class<HumanReadablePercentage> handledType() {
            return HumanReadablePercentage.class;
        }
    }

    static class HumanReadablePercentageDeserializer extends JsonDeserializer<HumanReadablePercentage> {
        @Override
        public HumanReadablePercentage deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return HumanReadablePercentage.of(p.getText());
        }

        @Override
        public Class<HumanReadablePercentage> handledType() {
            return HumanReadablePercentage.class;
        }
    }

    static class HumanReadableDurationSerializer extends JsonSerializer<HumanReadableDuration> {
        @Override
        public void serialize(HumanReadableDuration value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.toString());
        }

        @Override
        public Class<HumanReadableDuration> handledType() {
            return HumanReadableDuration.class;
        }
    }

    static class HumanReadableDurationDeserializer extends JsonDeserializer<HumanReadableDuration> {
        @Override
        public HumanReadableDuration deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return HumanReadableDuration.parse(p.getText());
        }

        @Override
        public Class<HumanReadableDuration> handledType() {
            return HumanReadableDuration.class;
        }
    }

    public static class HumanReadableNumberSerializer extends JsonSerializer<HumanReadableNumber> {
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

    public static class HumanReadableNumberDeserializer extends JsonDeserializer<HumanReadableNumber> {
        @Override
        public HumanReadableNumber deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return HumanReadableNumber.of(p.getText());
        }

        @Override
        public Class<?> handledType() {
            return HumanReadableNumber.class;
        }
    }
}
