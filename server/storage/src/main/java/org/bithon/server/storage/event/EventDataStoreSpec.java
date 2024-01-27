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

package org.bithon.server.storage.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.IMetricReader;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;

import java.util.Collections;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 27/1/24 11:44 am
 */
public class EventDataStoreSpec implements IDataStoreSpec {

    private final String store;

    @JsonIgnore
    private final IEventStorage storage;

    public EventDataStoreSpec(String store, IEventStorage storage) {
        this.store = store;
        this.storage = storage;
    }

    @Override
    public String getStore() {
        return store;
    }

    @Override
    public void setDataSourceSchema(DataSourceSchema schema) {
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public IDataStoreSpec withProperties(Map<String, String> properties) {
        return this;
    }

    @Override
    public IMetricReader createReader() {
        return storage.createReader();
    }
}
