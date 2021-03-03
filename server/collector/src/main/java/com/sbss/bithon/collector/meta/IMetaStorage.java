package com.sbss.bithon.collector.meta;

import java.util.Collection;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 9:49 上午
 */
public interface IMetaStorage {

    long getOrCreateMetadataId(String name, MetadataType type, long parent);

    Collection<Metadata> getMetadataByType(MetadataType type);

    void saveMetricDimension(String dataSource,
                             String dimensionName,
                             String dimensionValue, long timestamp);
}
