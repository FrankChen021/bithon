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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.Experimental;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author Frank Chen
 * @date 18/5/23 1:59 pm
 */
@Experimental
@Getter
public class DataStoreSpec {
    /**
     * internal | external
     */
    private final String type;

    private final Map<String, String> properties;

    @JsonCreator
    public DataStoreSpec(@JsonProperty("type") String type,
                         @JsonProperty("properties") Map<String, String> properties) {
        this.type = type;
        this.properties = properties == null ? Collections.emptyMap() : properties;
    }

    @JsonIgnore
    public boolean isInternal() {
        return "internal".equals(type);
    }

    @JsonIgnore
    public String getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataStoreSpec that = (DataStoreSpec) o;
        return Objects.equals(type, that.type) && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, properties);
    }

    public DataStoreSpec withProperties(Map<String, String> properties) {
        return new DataStoreSpec(this.type, properties);
    }
}
