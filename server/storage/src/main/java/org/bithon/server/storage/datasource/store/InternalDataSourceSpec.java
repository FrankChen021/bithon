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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author Frank Chen
 * @date 22/6/23 4:10 pm
 */
public class InternalDataSourceSpec implements IDataStoreSpec {

    private final String store;

    @JsonCreator
    public InternalDataSourceSpec(@JsonProperty("store") String store) {
        this.store = store;
    }

    @Override
    public String getType() {
        return "internal";
    }

    @Override
    public String getStore() {
        return store;
    }

    @JsonIgnore
    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public IDataStoreSpec withProperties(Map<String, String> properties) {
        return new InternalDataSourceSpec(store);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InternalDataSourceSpec that = (InternalDataSourceSpec) o;
        return Objects.equals(store, that.store);
    }

    @Override
    public int hashCode() {
        return Objects.hash(store);
    }
}
