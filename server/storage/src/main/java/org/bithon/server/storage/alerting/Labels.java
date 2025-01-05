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

package org.bithon.server.storage.alerting;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import org.bithon.component.commons.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * When an alert rule is based on GROUP-BY clause, this class holds the labels of a series
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/29 14:11
 */

@JsonSerialize(using = Labels.Serializer.class)
@JsonDeserialize(using = Labels.Deserializer.class)
public class Labels {
    private final List<String> values = new ArrayList<>();

    @Getter
    private String id = "";

    public Labels() {
    }

    public Labels(String id) {
        this.id = id;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public void add(String label, String value) {
        values.add(value);
        if (!id.isEmpty()) {
            id += ", ";
        }
        id += StringUtils.format("%s = '%s'", label, value);
    }

    public List<String> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Labels) {
            return id.equals(((Labels) obj).id);
        }
        return false;
    }

    public static class Serializer extends JsonSerializer<Labels> {
        @Override
        public void serialize(Labels labels, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(labels.id);
        }

        @Override
        public Class<Labels> handledType() {
            return Labels.class;
        }
    }

    public static class Deserializer extends JsonDeserializer<Labels> {
        @Override
        public Labels deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return new Labels(p.getValueAsString());
        }


        @Override
        public Class<Labels> handledType() {
            return Labels.class;
        }
    }
}
