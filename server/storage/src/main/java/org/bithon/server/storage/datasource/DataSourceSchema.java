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
import org.bithon.server.storage.datasource.dimension.IDimensionSpec;
import org.bithon.server.storage.datasource.dimension.LongDimensionSpec;
import org.bithon.server.storage.datasource.spec.CountMetricSpec;
import org.bithon.server.storage.datasource.spec.IMetricSpec;

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
    private final List<IDimensionSpec> dimensionsSpec;

    @Getter
    private final List<IMetricSpec> metricsSpec;

    @Getter
    private final JsonNode inputSourceSpec;

    /**
     * data source level ttl.
     * can be null.
     * If it's null, it's controlled by the global level TTL
     */
    @Getter
    private final Period ttl;

    @JsonIgnore
    private final Map<String, IDimensionSpec> dimensionMap = new HashMap<>(15);

    @JsonIgnore
    private final Map<String, IMetricSpec> metricsMap = new HashMap<>();

    /**
     * check a {timestamp, dimensions} are unique to help find out some internal wrong implementation
     */
    @Getter
    @Setter
    @JsonIgnore
    private boolean enforceDuplicationCheck = false;

    /**
     * a runtime property that holds the hash of the json formatted text of this object
     */
    @Getter
    @Setter
    @JsonIgnore
    private String signature;


    /**
     * a runtime property that the schema is only used for query.
     */
    @Getter
    @Setter
    @JsonIgnore
    private boolean isVirtual = false;

    private static IDimensionSpec TIMESTAMP_COLUMN = new LongDimensionSpec("timestamp", "timestamp", null, true, true);

    public DataSourceSchema(String displayText,
                            String name,
                            TimestampSpec timestampSpec,
                            List<IDimensionSpec> dimensionsSpec,
                            List<IMetricSpec> metricsSpec) {
        this(displayText, name, timestampSpec, dimensionsSpec, metricsSpec, null, null);
    }

    @JsonCreator
    public DataSourceSchema(@JsonProperty("displayText") @Nullable String displayText,
                            @JsonProperty("name") String name,
                            @JsonProperty("timestampSpec") @Nullable TimestampSpec timestampSpec,
                            @JsonProperty("dimensionsSpec") List<IDimensionSpec> dimensionsSpec,
                            @JsonProperty("metricsSpec") List<IMetricSpec> metricsSpec,
                            @JsonProperty("inputSourceSpec") @Nullable JsonNode inputSourceSpec,
                            @JsonProperty("ttl") @Nullable Period ttl) {
        this.displayText = displayText == null ? name : displayText;
        this.name = name;
        this.timestampSpec = timestampSpec == null ? new TimestampSpec("timestamp", "auto", null) : timestampSpec;
        this.dimensionsSpec = dimensionsSpec;
        this.metricsSpec = metricsSpec;
        this.inputSourceSpec = inputSourceSpec;
        this.ttl = ttl;

        this.dimensionsSpec.forEach((dimensionSpec) -> dimensionMap.put(dimensionSpec.getName(), dimensionSpec));
        this.metricsSpec.forEach((metricSpec) -> metricsMap.put(metricSpec.getName(), metricSpec));
        this.dimensionMap.put(TIMESTAMP_COLUMN.getName(), TIMESTAMP_COLUMN);

        // set owner after initialization
        this.metricsSpec.forEach((metricSpec) -> metricSpec.setOwner(this));
    }

    public IMetricSpec getMetricSpecByName(String name) {
        if (IMetricSpec.COUNT.equals(name)) {
            return CountMetricSpec.INSTANCE;
        }

        return metricsMap.get(name);
    }

    public boolean containsMetric(String name) {
        if (IMetricSpec.COUNT.equals(name)) {
            return true;
        }

        return metricsMap.containsKey(name);
    }

    public IDimensionSpec getDimensionSpecByName(String name) {
        return dimensionMap.get(name);
    }

    public IDimensionSpec getDimensionSpecByAlias(String alias) {
        for (IDimensionSpec dimSpec : this.dimensionsSpec) {
            if (alias.equals(dimSpec.getAlias())) {
                return dimSpec;
            }
        }
        return null;
    }

    public IColumnSpec getColumnByName(String name) {
        IDimensionSpec dimSpec = dimensionMap.get(name);
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
}
