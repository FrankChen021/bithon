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
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.bithon.component.commons.utils.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * When an alert rule is based on GROUP-BY clause, this class holds the labels of a series
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/29 14:11
 */

@JsonSerialize(using = Label.Serializer.class, keyUsing = Label.LabelKeySerializer.class)
@JsonDeserialize(using = Label.Deserializer.class, keyUsing = Label.LabelKeyDeserializer.class)
public class Label {
    public static final Label EMPTY = new Label(Collections.emptyMap());

    private final Map<String, String> kv;
    private transient int hashCode;
    private transient String id;

    public Label(Map<String, String> kv) {
        this.kv = kv;
        this.hashCode = kv.hashCode();
    }

    public boolean isEmpty() {
        return kv.isEmpty();
    }

    public int size() {
        return kv.size();
    }

    public String get(String key) {
        return kv.get(key);
    }

    public String formatIfNotEmpty(String format) {
        return kv.isEmpty() ? "" : StringUtils.format(format, this.toString());
    }

    @Override
    public String toString() {
        if (id == null) {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : this.kv.entrySet()) {
                if (!builder.isEmpty()) {
                    builder.append(',');
                }
                builder.append(entry.getKey());
                builder.append('=');
                builder.append(entry.getValue().replace(",", "&quote;"));
            }
            id = builder.toString();
        }
        return id;
    }

    @Override
    public synchronized int hashCode() {
        if (hashCode == 0) {
            hashCode = kv.hashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Label) {
            return kv.equals(((Label) obj).kv);
        }
        return false;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, String> kv = new TreeMap<>();

        public Builder add(String label, String value) {
            kv.put(label, value);
            return this;
        }

        public Label build() {
            return new Label(kv);
        }
    }

    public static class Serializer extends JsonSerializer<Label> {
        @Override
        public void serialize(Label label, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(label.toString());
        }

        @Override
        public Class<Label> handledType() {
            return Label.class;
        }
    }

    public static class Deserializer extends JsonDeserializer<Label> {
        @Override
        public Label deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            Map<String, String> kv = new TreeMap<>();
            StringUtils.extractKeyValueParis(p.getValueAsString(), ",", "=", (k, v) -> kv.put(k, v.replace("&quote;", ",")));
            return new Label(kv);
        }

        @Override
        public Class<Label> handledType() {
            return Label.class;
        }
    }

    public static class LabelKeySerializer extends JsonSerializer<Label> {
        @Override
        public void serialize(Label label, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeFieldName(label.toString());
        }
    }

    public static class LabelKeyDeserializer extends KeyDeserializer {

        @Override
        public Object deserializeKey(String s, DeserializationContext deserializationContext) throws IOException {
            Map<String, String> kv = new TreeMap<>();
            StringUtils.extractKeyValueParis(s, ",", "=", (k, v) -> kv.put(k, v.replace("&quote;", ",")));
            return new Label(kv);
        }
    }
}
