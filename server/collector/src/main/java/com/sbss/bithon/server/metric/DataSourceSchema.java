package com.sbss.bithon.server.metric;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sbss.bithon.server.metric.dimension.IDimensionSpec;
import com.sbss.bithon.server.metric.dimension.transformer.IDimensionTransformer;
import com.sbss.bithon.server.metric.filter.IFilter;
import com.sbss.bithon.server.metric.flatten.IFlattener;
import com.sbss.bithon.server.metric.aggregator.CountMetricSpec;
import com.sbss.bithon.server.metric.aggregator.IMetricSpec;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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

    @JsonCreator
    public DataSourceSchema(@JsonProperty("displayText") @Nullable String displayText,
                            @JsonProperty("name") @NotNull String name,
                            @JsonProperty("timestampSpec") @NotNull TimestampSpec timestampSpec,
                            @JsonProperty("dimensionsSpec") @NotNull @NotEmpty List<IDimensionSpec> dimensionsSpec,
                            @JsonProperty("metricsSpec") @NotNull @NotEmpty List<IMetricSpec> metricsSpec,
                            @JsonProperty("filtersSpec") @Nullable List<IFilter> filtersSpec,
                            @JsonProperty("flattenSpec") @Nullable List<IFlattener> flattenSpec) {
        this.displayText = displayText == null ? name : displayText;
        this.name = name;
        this.timestampSpec = timestampSpec;
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