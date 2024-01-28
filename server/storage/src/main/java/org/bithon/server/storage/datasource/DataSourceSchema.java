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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.bithon.server.commons.time.Period;
import org.bithon.server.storage.datasource.column.DateTimeColumn;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.column.LongColumn;
import org.bithon.server.storage.datasource.column.aggregatable.IAggregatableColumn;
import org.bithon.server.storage.datasource.column.aggregatable.count.AggregateCountColumn;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;

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
    private final List<IColumn> dimensionsSpec;

    @Getter
    private final List<IColumn> metricsSpec;

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
    @Setter
    private IDataStoreSpec dataStoreSpec;

    /**
     * Data source level ttl.
     * Can be null.
     * If it's null, it's controlled by the global level TTL
     */
    @Getter
    private final Period ttl;

    @JsonIgnore
    private final Map<String, IColumn> columnMap = new HashMap<>(17);

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

    private static final IColumn TIMESTAMP_COLUMN = new DateTimeColumn("timestamp", "timestamp");

    public DataSourceSchema(String displayText,
                            String name,
                            TimestampSpec timestampSpec,
                            List<IColumn> dimensionsSpec,
                            List<IColumn> metricsSpec) {
        this(displayText, name, timestampSpec, dimensionsSpec, metricsSpec, null, null, null, null);
    }

    @JsonCreator
    public DataSourceSchema(@JsonProperty("displayText") @Nullable String displayText,
                            @JsonProperty("name") String name,
                            @JsonProperty("timestampSpec") @Nullable TimestampSpec timestampSpec,
                            @JsonProperty("dimensionsSpec") List<IColumn> dimensionsSpec,
                            @JsonProperty("metricsSpec") List<IColumn> metricsSpec,
                            @JsonProperty("inputSourceSpec") @Nullable JsonNode inputSourceSpec,
                            @JsonProperty("storeSpec") @Nullable IDataStoreSpec dataStoreSpec,
                            @JsonProperty("ttl") @Nullable Period ttl,
                            @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this.displayText = displayText == null ? name : displayText;
        this.name = name;
        this.timestampSpec = timestampSpec == null ? new TimestampSpec("timestamp", "auto", null) : timestampSpec;
        this.dimensionsSpec = dimensionsSpec;
        this.metricsSpec = metricsSpec;
        this.inputSourceSpec = inputSourceSpec;
        this.dataStoreSpec = dataStoreSpec;
        this.ttl = ttl;

        this.dimensionsSpec.forEach((dimensionSpec) -> {
            columnMap.put(dimensionSpec.getName(), dimensionSpec);

            if (!dimensionSpec.getAlias().equals(dimensionSpec.getName())) {
                columnMap.put(dimensionSpec.getAlias(), dimensionSpec);
            }
        });

        this.metricsSpec.forEach((metricSpec) -> {
            columnMap.put(metricSpec.getName(), metricSpec);

            if (!metricSpec.getAlias().equals(metricSpec.getName())) {
                columnMap.put(metricSpec.getAlias(), metricSpec);
            }
        });

        this.columnMap.putIfAbsent(IAggregatableColumn.COUNT, AggregateCountColumn.INSTANCE);

        if ("timestamp".equals(this.timestampSpec.getTimestampColumn())) {
            this.columnMap.put(TIMESTAMP_COLUMN.getName(), TIMESTAMP_COLUMN);
        } else {
            this.columnMap.put(this.timestampSpec.getTimestampColumn(),
                               new LongColumn(this.timestampSpec.getTimestampColumn(),
                                              this.timestampSpec.getTimestampColumn()));
        }

        if (this.dataStoreSpec == null && objectMapper != null) {
            // Internally created schema does not have store
            try {
                this.dataStoreSpec = objectMapper.readValue("{\"type\": \"metric\"}", IDataStoreSpec.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        if (this.dataStoreSpec != null) {
            this.dataStoreSpec.setDataSourceSchema(this);
        }
    }

    public IColumn getColumnByName(String name) {
        return columnMap.get(name);
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
                                    this.ttl,
                                    null);
    }
}
