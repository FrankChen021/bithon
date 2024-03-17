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
import com.fasterxml.jackson.databind.JsonNode;
import org.bithon.server.commons.time.Period;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.DateTimeColumn;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.storage.datasource.column.aggregatable.count.AggregateCountColumn;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 27/1/24 12:13 pm
 */
public class EventTableSchema implements ISchema {

    public static ISchema createEventTableSchema(IEventStorage eventStorage) {
        return new EventTableSchema("event",
                                    eventStorage,
                                    Arrays.asList(new DateTimeColumn("timestamp", "timestamp"),
                                                  new StringColumn("appName", "appName"),
                                                  new StringColumn("instanceName", "instanceName"),
                                                  new StringColumn("type", "type"),
                                                  new StringColumn("arguments", "arguments"),
                                                  AggregateCountColumn.INSTANCE));
    }

    private final String name;
    private final TimestampSpec timestampSpec = new TimestampSpec("timestamp", "auto", null);
    private final Map<String, IColumn> columnMap = new HashMap<>();
    private final IDataStoreSpec dataStoreSpec;

    public EventTableSchema(String name, IEventStorage storage, List<IColumn> columns) {
        this.name = name;
        this.dataStoreSpec = new EventDataStoreSpec("bithon_event", storage);

        columns.forEach((column) -> {
            columnMap.put(column.getName(), column);

            if (!column.getAlias().equals(column.getName())) {
                columnMap.put(column.getAlias(), column);
            }
        });
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayText() {
        return name;
    }

    @Override
    public TimestampSpec getTimestampSpec() {
        return timestampSpec;
    }

    @Override
    public IColumn getColumnByName(String name) {
        return this.columnMap.get(name);
    }

    @Override
    public Collection<IColumn> getColumns() {
        return this.columnMap.values();
    }

    @Override
    public JsonNode getInputSourceSpec() {
        return null;
    }

    @Override
    public IDataStoreSpec getDataStoreSpec() {
        return this.dataStoreSpec;
    }

    @Override
    public ISchema withDataStore(IDataStoreSpec spec) {
        return null;
    }

    @Override
    public void setSignature(String signature) {
    }

    @Override
    public String getSignature() {
        return null;
    }

    @Override
    public Period getTtl() {
        return null;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    static class EventDataStoreSpec implements IDataStoreSpec {

        private final String store;

        @JsonIgnore
        private final IEventStorage storage;

        EventDataStoreSpec(String store, IEventStorage storage) {
            this.store = store;
            this.storage = storage;
        }

        @Override
        public String getStore() {
            return store;
        }

        @Override
        public void setSchema(ISchema dataSource) {
        }

        @Override
        public boolean isInternal() {
            return true;
        }

        @Override
        public IDataSourceReader createReader() {
            return storage.createReader();
        }
    }
}
