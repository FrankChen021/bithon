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

package org.bithon.server.datasource.store;

import lombok.Getter;
import org.bithon.component.commons.Experimental;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.datasource.ISchema;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author Frank Chen
 * @date 18/5/23 1:59 pm
 */
@Experimental
@Getter
public abstract class ExternalDataStoreSpec implements IDataStoreSpec {

    protected final Map<String, Object> properties;
    protected final String store;

    public ExternalDataStoreSpec(Map<String, Object> properties,
                                 String store) {
        this.properties = properties == null ? Collections.emptyMap() : properties;
        this.store = store;
    }

    @Override
    public String getStore() {
        return store;
    }

    @Override
    public void setSchema(ISchema schema) {
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    protected Map<String, Object> getSensitiveHiddenProps() {
        if (CollectionUtils.isNotEmpty(properties)) {
            Map<String, Object> newProps = new TreeMap<>(this.properties);
            newProps.computeIfPresent("password", (k, old) -> "<HIDDEN>");
            return newProps;
        }
        return this.properties;
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
