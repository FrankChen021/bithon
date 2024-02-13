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
import com.fasterxml.jackson.databind.JsonNode;
import org.bithon.server.commons.time.Period;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.storage.datasource.column.aggregatable.count.AggregateCountColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;
import org.bithon.server.storage.tracing.index.TagIndexConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 29/1/24 10:14 am
 */
public class TraceTableSchema implements ISchema {

    private final TimestampSpec timestampSpec = new TimestampSpec("timestamp", "auto", null);
    private final String name;

    private final IDataStoreSpec dataStoreSpec;
    private final Map<String, IColumn> columnMap = new HashMap<>();
    private final Map<String, IColumn> aliasMap = new HashMap<>();

    TraceTableSchema(String name, ITraceStorage storage, List<IColumn> columns) {
        this.name = name;
        this.dataStoreSpec = new TraceDataStore("bithon_" + name, storage);

        columns.forEach((column) -> {
            columnMap.put(column.getName(), column);

            if (!column.getAlias().equals(column.getName())) {
                aliasMap.put(column.getAlias(), column);
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
        IColumn column = this.columnMap.get(name);
        return column == null ? this.aliasMap.get(name) : column;
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

    public static TraceTableSchema createSummaryTableSchema(ITraceStorage traceStorage) {
        return new TraceTableSchema("trace_span_summary",
                                    traceStorage,
                                    Arrays.asList(new StringColumn("appName",
                                                                   "appName"),
                                                  new StringColumn("instanceName",
                                                                   "instanceName"),
                                                  new StringColumn("status",
                                                                   "status"),
                                                  new StringColumn("name", "name"),
                                                  new StringColumn("normalizedUrl",
                                                                   "url"),
                                                  new StringColumn("kind", "kind"),

                                                  AggregateCountColumn.INSTANCE,
                                                  // microsecond
                                                  new AggregateLongSumColumn("costTimeMs",
                                                                             "costTimeMs"))
        );
    }

    public static TraceTableSchema createIndexTableSchema(ITraceStorage traceStorage, TagIndexConfig tagIndexConfig) {
        List<IColumn> dimensionSpecs = new ArrayList<>();
        if (tagIndexConfig != null) {
            for (Map.Entry<String, Integer> entry : tagIndexConfig.getMap().entrySet()) {
                String tagName = "tags." + entry.getKey();
                Integer indexPos = entry.getValue();
                dimensionSpecs.add(new StringColumn("f" + indexPos,
                                                    // Alias
                                                    tagName));
            }
        }

        dimensionSpecs.add(AggregateCountColumn.INSTANCE);
        return new TraceTableSchema("trace_span_tag_index",
                                    traceStorage,
                                    dimensionSpecs);

    }

    static class TraceDataStore implements IDataStoreSpec {

        private final String store;

        @JsonIgnore
        private final ITraceStorage storage;

        TraceDataStore(String store, ITraceStorage storage) {
            this.store = store;
            this.storage = storage;
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
            return true;
        }

        @Override
        public IDataSourceReader createReader() {
            return storage.createReader();
        }
    }

}
