package com.sbss.bithon.collector.meta.builtin;

import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.collector.meta.Metadata;
import com.sbss.bithon.collector.meta.MetadataType;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 9:57 上午
 */
public class BuiltinMetaStorage implements IMetaStorage {

    private final AtomicLong id = new AtomicLong();
    private final Map<MetadataType, Map<String, Metadata>> metadata = new ConcurrentHashMap<>();

    @Override
    public long getOrCreateMetadataId(String name,
                                      MetadataType type,
                                      long parent) {
        return metadata.computeIfAbsent(type, key -> new ConcurrentHashMap<>())
            .computeIfAbsent(name, key -> {
                Metadata d = new Metadata(name, type, parent);
                d.setId(id.incrementAndGet());
                return d;
            }).getId();
    }

    @Override
    public Collection<Metadata> getMetadataByType(MetadataType type) {
        Map<String, Metadata> metaOfType = metadata.get(type);
        return metaOfType == null ? Collections.emptyList() : metaOfType.values();
    }
}
