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

package org.bithon.server.storage.tracing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.server.datasource.input.IInputRow;
import org.bithon.server.storage.common.ApplicationType;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Inherits from {@link IInputRow} to support extract metrics over span logs
 * Maybe change this class into a subclass of {@link HashMap} is a better solution
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:28 下午
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TraceSpan implements IInputRow {

    public static class TraceSpanDeserializer extends StdDeserializer<TraceSpan> implements ResolvableDeserializer {
        private final JsonDeserializer<TraceSpan> deserializer;

        public TraceSpanDeserializer(JsonDeserializer<TraceSpan> deserializer) {
            super(TraceSpan.class);
            this.deserializer = deserializer;
        }

        @Override
        public TraceSpan deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            TraceSpan span = deserializer.deserialize(p, ctxt);
            if (span.tags == null) {
                // Make sure 'tags' is not null
                span.tags = new TreeMap<>();
            }
            return span;
        }

        @Override
        public void resolve(DeserializationContext ctxt) throws JsonMappingException {
            if (deserializer instanceof ResolvableDeserializer) {
                ((ResolvableDeserializer) deserializer).resolve(ctxt);
            }
        }
    }

    public static class TagDeserializer extends JsonDeserializer<TreeMap<String, String>> {
        public static final TypeReference<TreeMap<String, String>> TYPE = new TypeReference<>() {
        };

        @Override
        public TreeMap<String, String> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return p.readValueAs(TYPE);
        }
    }

    private static Map<String, FieldAccessor> fieldAccessors = new HashMap<>();

    static {
        for (Field field : TraceSpan.class.getDeclaredFields()) {
            if (field.getAnnotation(JsonIgnore.class) != null) {
                // ignore runtime property
                continue;
            }

            field.setAccessible(true);
            fieldAccessors.put(field.getName(), new FieldAccessor(field));
        }
    }

    public String appName;
    public String instanceName;
    /**
     * UNKNOWN
     * JAVA
     */
    public String appType = ApplicationType.UNKNOWN;
    public String traceId;
    public String spanId;
    public String kind;
    public String parentSpanId;
    public String parentApplication;

    /**
     * The deserializer is customized because the default deserializer returns an unmodifiable map
     */
    @JsonDeserialize(using = TagDeserializer.class)
    public Map<String, String> tags;

    /**
     * in microseconds
     */
    public long costTime;

    /**
     * in microsecond
     */
    public long startTime;
    public long endTime;
    public String name;
    public String clazz;
    public String method;
    public String status = "";
    public String normalizedUri = "";

    @JsonIgnore
    private Map<String, Object> properties;

    public String getTag(String name) {
        return tags.get(name);
    }

    public void setTag(String name, String value) {
        this.tags.put(name, value);
    }

    /**
     * This is ONLY used for serialization
     * so that the FE uses the same field name as the name defined in the data source layer.
     * Historically, the name is NOT correct, it's in microsecond, NOT milliseconds as suggested by the name
     */
    @JsonProperty("costTimeMs")
    public long getCostTimeMs() {
        return costTime;
    }

    /**
     * This is ONLY used for serialization
     * so that the FE uses the same field name as the name defined in the data source layer
     */
    @JsonProperty("startTimeUs")
    public long getStartTimeUs() {
        return startTime;
    }

    @Override
    public String toString() {
        return "TraceSpan{" +
               "appName='" + appName + '\'' +
               ", instanceName='" + instanceName + '\'' +
               ", traceId='" + traceId + '\'' +
               ", spanId='" + spanId + '\'' +
               ", kind='" + kind + '\'' +
               ", parentSpanId='" + parentSpanId + '\'' +
               ", parentApplication='" + parentApplication + '\'' +
               ", tags=" + tags +
               ", costTime=" + costTime +
               ", startTime=" + startTime +
               ", endTime=" + endTime +
               ", name='" + name + '\'' +
               ", clazz='" + clazz + '\'' +
               ", method='" + method + '\'' +
               '}';
    }

    @Override
    public Object getCol(String columnName) {
        FieldAccessor accessor = fieldAccessors.get(columnName);
        if (accessor != null) {
            Object val = accessor.get(this);
            if (val != null) {
                return val;
            }
        }
        return properties == null ? null : properties.get(columnName);
    }

    @Override
    public void updateColumn(String name, Object value) {
        FieldAccessor accessor = fieldAccessors.get(name);
        if (accessor != null) {
            accessor.set(this, value);
            return;
        }
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(name, value);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("appName", appName);
        map.put("instanceName", instanceName);
        map.put("appType", appType);
        map.put("traceId", traceId);
        map.put("spanId", spanId);
        map.put("kind", kind);
        map.put("parentSpanId", parentSpanId);
        map.put("parentApplication", parentApplication);
        map.put("tags", new HashMap<>(tags));
        map.put("costTime", costTime);
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        map.put("name", name);
        map.put("clazz", clazz);
        map.put("method", method);
        map.put("status", status);
        map.put("normalizedUri", normalizedUri);
        return map;
    }

    static class FieldAccessor {
        private final Field field;

        public FieldAccessor(Field field) {
            this.field = field;
        }

        Object get(TraceSpan span) {
            try {
                return field.get(span);
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        void set(TraceSpan span, Object val) {
            try {
                field.set(span, val);
            } catch (IllegalAccessException ignored) {
            }
        }
    }
}
