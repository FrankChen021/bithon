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

package org.bithon.server.storage.tracing.datasource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;
import org.bithon.server.storage.tracing.ITraceStorage;

import java.util.Collections;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 26/1/24 3:43 pm
 */
class TraceDataStore implements IDataStoreSpec {

    private final String store;

    @JsonIgnore
    private final ITraceStorage storage;

    public TraceDataStore(String store, ITraceStorage storage) {
        this.store = store;
        this.storage = storage;
    }

    @Override
    public String getStore() {
        return store;
    }

    @Override
    public void setDataSourceSchema(ISchema schema) {
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public IDataStoreSpec withProperties(Map<String, Object> properties) {
        return this;
    }

    @Override
    public IDataSourceReader createReader() {
        return storage.createReader();
    }
}
