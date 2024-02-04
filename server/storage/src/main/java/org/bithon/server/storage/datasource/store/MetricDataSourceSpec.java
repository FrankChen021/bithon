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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.metrics.IMetricStorage;

import java.util.Objects;

/**
 * @author Frank Chen
 * @date 22/6/23 4:10 pm
 */
public class MetricDataSourceSpec implements IDataStoreSpec {

    @JsonIgnore
    private String store;

    @JsonIgnore
    private final IMetricStorage storage;
    @JsonIgnore
    private ISchema schema;

    public MetricDataSourceSpec(@JacksonInject(useInput = OptBoolean.FALSE) IMetricStorage storage) {
        this.storage = storage;
    }

    @Override
    public String getStore() {
        return store;
    }

    @Override
    public void setSchema(ISchema schema) {
        this.store = "bithon_" + schema.getName().replaceAll("-", "_");
        this.schema = schema;
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public IDataSourceReader createReader() {
        return storage.createMetricReader(schema);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetricDataSourceSpec that = (MetricDataSourceSpec) o;
        return Objects.equals(store, that.store);
    }

    @Override
    public int hashCode() {
        return Objects.hash(store);
    }
}
