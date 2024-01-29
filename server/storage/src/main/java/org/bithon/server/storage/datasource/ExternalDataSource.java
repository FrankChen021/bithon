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
import org.bithon.server.storage.datasource.column.DateTimeColumn;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.column.LongColumn;
import org.bithon.server.storage.datasource.column.aggregatable.IAggregatableColumn;
import org.bithon.server.storage.datasource.column.aggregatable.count.AggregateCountColumn;
import org.bithon.server.storage.datasource.store.ExternalDataStoreSpec;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 */
public class ExternalDataSource implements IDataSource {
    @Getter
    private final String name;

    @Getter
    private final TimestampSpec timestampSpec;

    @Getter
    private final List<IColumn> dimensionsSpec;

    @Getter
    private final List<IColumn> metricsSpec;

    /**
     * Experimental
     */
    @Getter
    @Setter
    private IDataStoreSpec dataStoreSpec;

    @JsonIgnore
    private final Map<String, IColumn> columnMap = new HashMap<>(17);

    private static final IColumn TIMESTAMP_COLUMN = new DateTimeColumn("timestamp", "timestamp");

    @JsonCreator
    public ExternalDataSource(@JsonProperty("name") String name,
                              @JsonProperty("timestampSpec") @Nullable TimestampSpec timestampSpec,
                              @JsonProperty("dimensionsSpec") List<IColumn> dimensionsSpec,
                              @JsonProperty("metricsSpec") List<IColumn> metricsSpec,
                              @JsonProperty("storeSpec") @Nullable ExternalDataStoreSpec dataStoreSpec) {
        this.name = name;
        this.timestampSpec = timestampSpec == null ? new TimestampSpec("timestamp", "auto", null) : timestampSpec;
        this.dimensionsSpec = dimensionsSpec;
        this.metricsSpec = metricsSpec;
        this.dataStoreSpec = dataStoreSpec;

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

        if (this.dataStoreSpec != null) {
            this.dataStoreSpec.setDataSourceSchema(this);
        }
    }

    @Override
    public String getDisplayText() {
        return null;
    }

    public IColumn getColumnByName(String name) {
        return columnMap.get(name);
    }

    @Override
    public Collection<IColumn> getColumns() {
        return null;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof ExternalDataSource) {
            return ((ExternalDataSource) rhs).getName().equals(this.name);
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

    public ExternalDataSource withDataStore(IDataStoreSpec dataStoreSpec) {
        return new ExternalDataSource(this.name,
                                      this.timestampSpec,
                                      this.dimensionsSpec,
                                      this.metricsSpec,
                                      (ExternalDataStoreSpec) dataStoreSpec);
    }

    @Override
    public JsonNode getInputSourceSpec() {
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
}
