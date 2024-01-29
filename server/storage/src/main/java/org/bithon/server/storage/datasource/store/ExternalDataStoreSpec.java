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

package org.bithon.server.storage.datasource.store;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.bithon.component.commons.Experimental;
import org.bithon.server.storage.datasource.IDataSource;
import org.bithon.server.storage.datasource.query.IDataSourceReader;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Frank Chen
 * @date 18/5/23 1:59 pm
 */
@Experimental
@Getter
public class ExternalDataStoreSpec implements IDataStoreSpec {

    private final Map<String, String> properties;
    private final String store;

    @JsonIgnore
    private final ObjectMapper objectMapper;

    @JsonCreator
    public ExternalDataStoreSpec(@JsonProperty("properties") Map<String, String> properties,
                                 @JsonProperty("store") String store,
                                 @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this.properties = properties == null ? Collections.emptyMap() : properties;
        this.store = store;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getStore() {
        return store;
    }

    @Override
    public void setDataSourceSchema(IDataSource schema) {
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    public ExternalDataStoreSpec withProperties(Map<String, String> properties) {
        return new ExternalDataStoreSpec(properties, store, objectMapper);
    }

    @Override
    public IDataSourceReader createReader() throws IOException {
        Map<String, Object> args = new HashMap<>();

        // Only JDBC type is supported now
        // In the future, if the external source is extended,
        // the 'type' property should be provided in the spec
        args.put("type", "jdbc");
        args.put("name", store);
        args.put("props", this.properties);
        return objectMapper.readValue(objectMapper.writeValueAsBytes(args), IDataSourceReader.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExternalDataStoreSpec that = (ExternalDataStoreSpec) o;
        return Objects.equals(properties, that.properties) && Objects.equals(store, that.store);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties, store);
    }
}
