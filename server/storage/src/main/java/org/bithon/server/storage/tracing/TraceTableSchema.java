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
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.column.IColumn;
import org.bithon.server.datasource.column.LongColumn;
import org.bithon.server.datasource.column.ObjectColumn;
import org.bithon.server.datasource.column.StringColumn;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.store.IDataStoreSpec;
import org.bithon.server.storage.tracing.index.TagIndexConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 29/1/24 10:14 am
 */
public class TraceTableSchema implements ISchema {

    public static final String TRACE_SPAN_SUMMARY_SCHEMA_NAME = "trace_span_summary";
    public static final String TRACE_SPAN_SCHEMA_NAME = "trace_span";
    public static final String TRACE_SPAN_TAG_INDEX_SCHEMA_NAME = "trace_span_tag_index";
    private final TimestampSpec timestampSpec = new TimestampSpec("timestamp");
    private final String name;

    private final IDataStoreSpec dataStoreSpec;

    // Use LinkedHashMap to keep the order of the input
    private final Map<String, IColumn> columnMap = new LinkedHashMap<>();
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

    @Override
    public boolean isVirtual() {
        return true;
    }

    public static TraceTableSchema createTraceSpanSummaryTableSchema(ITraceStorage traceStorage) {
        return new TraceTableSchema(TRACE_SPAN_SUMMARY_SCHEMA_NAME,
                                    traceStorage,
                                    getTraceSpanColumns());
    }

    public static TraceTableSchema createTraceSpanTableSchema(ITraceStorage traceStorage) {
        return new TraceTableSchema(TRACE_SPAN_SCHEMA_NAME,
                                    traceStorage,
                                    getTraceSpanColumns());
    }

    private static List<IColumn> getTraceSpanColumns() {
        return Arrays.asList(new StringColumn("traceId", "traceId"),
                             new StringColumn("appName",
                                              "appName"),
                             new StringColumn("instanceName",
                                              "instanceName"),
                             new StringColumn("status",
                                              "status"),
                             new StringColumn("name", "name"),
                             new StringColumn("clazz", "clazz"),
                             new StringColumn("method", "method"),
                             new StringColumn("normalizedUrl", "url"),
                             new StringColumn("kind", "kind"),
                             new ObjectColumn("attributes", "tags"),

                             // microsecond
                             new LongColumn("costTimeMs", "costTimeMs"),
                             new LongColumn("startTimeUs", "startTimeUs"));
    }

    public static TraceTableSchema createIndexTableSchema(ITraceStorage traceStorage, TagIndexConfig tagIndexConfig) {
        List<IColumn> dimensionSpecs = new ArrayList<>();
        if (tagIndexConfig != null) {
            for (Map.Entry<String, Integer> entry : tagIndexConfig.getMap().entrySet()) {
                String tagName = entry.getKey();
                Integer indexPos = entry.getValue();
                dimensionSpecs.add(new StringColumn("f" + indexPos,
                                                    // Alias
                                                    tagName));
            }
        }

        return new TraceTableSchema(TRACE_SPAN_TAG_INDEX_SCHEMA_NAME,
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
