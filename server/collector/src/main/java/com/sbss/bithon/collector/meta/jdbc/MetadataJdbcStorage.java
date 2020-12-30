package com.sbss.bithon.collector.meta.jdbc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.collector.meta.Metadata;
import com.sbss.bithon.collector.meta.MetadataType;
import com.sbss.bithon.component.db.dao.MetadataDAO;
import org.jooq.DSLContext;

import java.time.Duration;
import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/11 10:56 下午
 */
public class MetadataJdbcStorage implements IMetaStorage {

    private final MetadataDAO metadataDao;
    private final Cache<Metadata, Long> metaCache;

    public MetadataJdbcStorage(DSLContext dsl) {
        this.metadataDao = new MetadataDAO(dsl);
        metaCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).build();
    }

    public long getOrCreateApplicationId(String name) {
        return getOrCreateMetadataId(name, MetadataType.APPLICATION, 0L);
    }

    @Override
    public long getOrCreateMetadataId(String name, MetadataType type, long parent) {
        Metadata key = new Metadata(name, type, parent);
        Long id = metaCache.getIfPresent(key);
        if (id == null) {
            id = metadataDao.insertMetadata(name, type.getType(), parent);
            metaCache.put(key, id);
        }
        return id;
    }

    @Override
    public Collection<Metadata> getMetadataByType(MetadataType type) {
        return null;
    }
}
