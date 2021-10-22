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

package org.bithon.server.metric;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.bithon.server.common.utils.datetime.Period;
import org.bithon.server.metric.aggregator.spec.CountMetricSpec;
import org.bithon.server.metric.aggregator.spec.IMetricSpec;
import org.bithon.server.metric.dimension.IDimensionSpec;
import org.bithon.server.metric.dimension.transformer.IDimensionTransformer;
import org.bithon.server.metric.filter.IFilter;
import org.bithon.server.metric.flatten.IFlattener;

import javax.annotation.Nullable;
import java.util.Collections;
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
    private final List<IFilter> filtersSpec;

    @Getter
    private final List<IFlattener> flattenSpec;

    @Getter
    @JsonIgnore
    private final Map<String, IDimensionTransformer> dimensionTransformers = new HashMap<>();

    @JsonIgnore
    private final Map<String, IDimensionSpec> dimensionMap = new HashMap<>();

    @JsonIgnore
    private final Map<String, IMetricSpec> metricsMap = new HashMap<>();

    @Getter
    @Setter
    private Period ttl;

    /**
     * check a {timestamp, dimensions} are unique to help find out some internal wrong implementation
     */
    @Getter
    @Setter
    private boolean enforceDuplicationCheck = true;

    @JsonCreator
    public DataSourceSchema(@JsonProperty("displayText") @Nullable String displayText,
                            @JsonProperty("name") String name,
                            @JsonProperty("timestampSpec") @Nullable TimestampSpec timestampSpec,
                            @JsonProperty("dimensionsSpec") List<IDimensionSpec> dimensionsSpec,
                            @JsonProperty("metricsSpec") List<IMetricSpec> metricsSpec,
                            @JsonProperty("filtersSpec") @Nullable List<IFilter> filtersSpec,
                            @JsonProperty("flattenSpec") @Nullable List<IFlattener> flattenSpec,
                            @JsonProperty("ttl") @Nullable Period ttl) {
        this.ttl = ttl == null ? new Period("P1D") : ttl;
        this.displayText = displayText == null ? name : displayText;
        this.name = name;
        this.timestampSpec = timestampSpec == null ? new TimestampSpec("timestamp", "auto", null) : timestampSpec;
        this.dimensionsSpec = dimensionsSpec;
        this.metricsSpec = metricsSpec;
        this.filtersSpec = filtersSpec == null ? Collections.emptyList() : filtersSpec;
        this.flattenSpec = flattenSpec == null ? Collections.emptyList() : flattenSpec;

        for (IDimensionSpec dimensionSpec : this.dimensionsSpec) {
            dimensionMap.put(dimensionSpec.getName(), dimensionSpec);
            if (dimensionSpec.getTransformer() != null) {
                dimensionTransformers.put(dimensionSpec.getName(), dimensionSpec.getTransformer());
            }
        }
        this.metricsSpec.forEach((metricSpec) -> metricsMap.put(metricSpec.getName(), metricSpec));

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

    public Map<String, IDimensionSpec> cloneDimensions() {
        return new HashMap<>(this.dimensionMap);
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
}
