package com.sbss.bithon.collector.meta.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sbss.bithon.collector.common.pojo.DisplayableText;
import com.sbss.bithon.collector.meta.EndPointLink;
import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.collector.meta.Metadata;
import com.sbss.bithon.collector.meta.MetadataType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Duration;
import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/4 12:15 上午
 */
public class CachableMetadataStorage implements IMetaStorage {

    @Getter
    @EqualsAndHashCode
    @AllArgsConstructor
    static class DimensionValue {
        private final String dataSource;
        private final String name;
        private final String value;
    }

    private final IMetaStorage delegate;

    // TODO: replace with LoadingCache
    private final Cache<Metadata, Long> metaCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).build();
    private final Cache<DimensionValue, Long> dimensionCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).build();
    private final Cache<EndPointLink, Long> topoCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).build();
    private final Cache<String, String> instanceCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(5)).build();

    public CachableMetadataStorage(IMetaStorage delegate) {
        this.delegate = delegate;
    }

    // TODO:
    //
    // 控制写入TPS
    // 中间采用一个队列，新数据覆盖队列中的数据（时间）
    @Override
    public long getOrCreateMetadataId(String name, MetadataType type, long parent) {
        Metadata key = new Metadata(name, type, parent);
        Long id = metaCache.getIfPresent(key);
        if (id == null) {
            id = delegate.getOrCreateMetadataId(name, type, parent);
            metaCache.put(key, id);
        }
        return id;
    }

    @Override
    public Collection<Metadata> getMetadataByType(MetadataType type) {
        return null;
    }

    @Override
    public long createMetricDimension(String dataSource,
                                      String dimensionName,
                                      String dimensionValue,
                                      long timestamp) {
        DimensionValue key = new DimensionValue(dataSource, dimensionName, dimensionValue);
        Long id = dimensionCache.getIfPresent(key);
        if (id == null) {
            id = delegate.createMetricDimension(dataSource, dimensionName, dimensionValue, timestamp);
            dimensionCache.put(key, id);
        }
        return id;
    }

    @Override
    public long createTopo(EndPointLink link) {
        Long id = topoCache.getIfPresent(link);
        if (id == null) {
            id = delegate.createTopo(link);
            topoCache.put(link, id);
        }
        return id;
    }

    @Override
    public String getApplicationByInstance(String instanceName) {
        String applicationName = instanceCache.getIfPresent(instanceName);
        if (applicationName == null) {
            applicationName = delegate.getApplicationByInstance(instanceName);
            if (applicationName != null) {
                instanceCache.put(instanceName, applicationName);
            }
            return applicationName;
        }
        return applicationName;
    }

    @Override
    public boolean isApplicationExist(String applicationName) {
        //TODO: cache
        return delegate.isApplicationExist(applicationName);
    }

    @Override
    public Collection<DisplayableText> getMetricDimensions(String dataSourceName, String dimensionName, String startISO8601, String endISO8601) {
        return null;
    }
}
