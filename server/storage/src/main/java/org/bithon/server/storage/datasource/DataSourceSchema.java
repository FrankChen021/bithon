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

package org.bithon.server.storage.datasource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.bithon.server.commons.time.Period;
import org.bithon.server.storage.datasource.column.IColumnSpec;
import org.bithon.server.storage.datasource.column.LongColumnSpec;
import org.bithon.server.storage.datasource.column.aggregatable.count.AggregateCountColumnSpec;
import org.bithon.server.storage.datasource.column.aggregatable.IAggregatableColumnSpec;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;
import org.bithon.server.storage.datasource.store.InternalDataSourceSpec;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 */
public class DataSourceSchema {
    @Getter
    private final String displayText;

    @Getter
    private final String name;

    @Getter
    private final TimestampSpec timestampSpec;

    @Getter
    private final List<IColumnSpec> dimensionsSpec;

    @Getter
    private final List<IColumnSpec> metricsSpec;

    /**
     * Where the data should be ingested from.
     * It's the caller to deserialize the object
     */
    @Getter
    private final JsonNode inputSourceSpec;

    /**
     * Experimental
     */
    @Getter
    private final IDataStoreSpec dataStoreSpec;

    /**
     * Data source level ttl.
     * Can be null.
     * If it's null, it's controlled by the global level TTL
     */
    @Getter
    private final Period ttl;

    @JsonIgnore
    private final Map<String, IColumnSpec> dimensionMap = new HashMap<>(15);

    @JsonIgnore
    private final Map<String, IColumnSpec> metricsMap = new HashMap<>();

    /**
     * check a {timestamp, dimensions} are unique to help find out some internal wrong implementation
     */
    @Getter
    @Setter
    @JsonIgnore
    private boolean enforceDuplicationCheck = false;

    /**
     * A runtime property that holds the hash of the json formatted text of this object
     */
    @Getter
    @Setter
    @JsonIgnore
    private String signature;


    /**
     * A runtime property that the schema is only used for queries.
     */
    @Getter
    @Setter
    @JsonIgnore
    private boolean isVirtual = false;

    private static final IColumnSpec TIMESTAMP_COLUMN = new LongColumnSpec("timestamp", "timestamp", null, true);

    public DataSourceSchema(String displayText,
                            String name,
                            TimestampSpec timestampSpec,
                            List<IColumnSpec> dimensionsSpec,
                            List<IColumnSpec> metricsSpec) {
        this(displayText, name, timestampSpec, dimensionsSpec, metricsSpec, null, null, null);
    }

    @JsonCreator
    public DataSourceSchema(@JsonProperty("displayText") @Nullable String displayText,
                            @JsonProperty("name") String name,
                            @JsonProperty("timestampSpec") @Nullable TimestampSpec timestampSpec,
                            @JsonProperty("dimensionsSpec") List<IColumnSpec> dimensionsSpec,
                            @JsonProperty("metricsSpec") List<IColumnSpec> metricsSpec,
                            @JsonProperty("inputSourceSpec") @Nullable JsonNode inputSourceSpec,
                            @JsonProperty("storeSpec") @Nullable IDataStoreSpec dataStoreSpec,
                            @JsonProperty("ttl") @Nullable Period ttl) {
        this.displayText = displayText == null ? name : displayText;
        this.name = name;
        this.timestampSpec = timestampSpec == null ? new TimestampSpec("timestamp", "auto", null) : timestampSpec;
        this.dimensionsSpec = dimensionsSpec;
        this.metricsSpec = metricsSpec;
        this.inputSourceSpec = inputSourceSpec;
        this.dataStoreSpec = dataStoreSpec == null ? new InternalDataSourceSpec("bithon_" + name.replaceAll("-", "_")) : dataStoreSpec;
        this.ttl = ttl;

        this.dimensionsSpec.forEach((dimensionSpec) -> dimensionMap.put(dimensionSpec.getName(), dimensionSpec));
        this.metricsSpec.forEach((metricSpec) -> metricsMap.put(metricSpec.getName(), metricSpec));
        if ("timestamp".equals(timestampSpec.getTimestampColumn())) {
            this.dimensionMap.put(TIMESTAMP_COLUMN.getName(), TIMESTAMP_COLUMN);
        } else {
            this.dimensionMap.put(timestampSpec.getTimestampColumn(), new LongColumnSpec(timestampSpec.getTimestampColumn(), timestampSpec.getTimestampColumn(), null, true));
        }
    }

    public IColumnSpec getMetricSpecByName(String name) {
        if (IAggregatableColumnSpec.COUNT.equals(name)) {
            return AggregateCountColumnSpec.INSTANCE;
        }

        return metricsMap.get(name);
    }

    public boolean containsMetric(String name) {
        if (IAggregatableColumnSpec.COUNT.equals(name)) {
            return true;
        }

        return metricsMap.containsKey(name);
    }

    public IColumnSpec getDimensionSpecByName(String name) {
        return dimensionMap.get(name);
    }

    public IColumnSpec getDimensionSpecByAlias(String alias) {
        for (IColumnSpec dimSpec : this.dimensionsSpec) {
            if (alias.equals(dimSpec.getAlias())) {
                return dimSpec;
            }
        }
        return null;
    }

    public IColumnSpec getColumnByName(String name) {
        IColumnSpec dimSpec = dimensionMap.get(name);
        return dimSpec == null ? getMetricSpecByName(name) : dimSpec;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof DataSourceSchema) {
            return ((DataSourceSchema) rhs).getName().equals(this.name);
        } else {
            return false;
        }
    }

    /**
     * helps debugging
     */
    @Override
    public String toString() {
        return this.name;
    }

    public DataSourceSchema withDataStore(IDataStoreSpec dataStoreSpec) {
        return new DataSourceSchema(this.displayText,
                                    this.name,
                                    this.timestampSpec,
                                    this.dimensionsSpec,
                                    this.metricsSpec,
                                    this.inputSourceSpec,
                                    dataStoreSpec,
                                    this.ttl);
    }
}
